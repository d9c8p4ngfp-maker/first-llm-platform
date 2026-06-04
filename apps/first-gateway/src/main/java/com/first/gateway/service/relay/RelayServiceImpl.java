package com.first.gateway.service.relay;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.first.gateway.config.AiServiceProperties;
import com.first.gateway.domain.entity.Channel;
import com.first.gateway.domain.entity.UserMemory;
import com.first.gateway.domain.entity.UserProfile;
import com.first.gateway.infra.ai.AiServiceClient;
import com.first.gateway.infra.crypto.ChannelKeyCrypto;
import com.first.gateway.infra.error.GatewayError;
import com.first.gateway.infra.error.GatewayException;
import com.first.gateway.relay.adapter.OpenAiAdapter;
import com.first.gateway.relay.router.ChannelHealthTracker;
import com.first.gateway.relay.router.ChannelSelection;
import com.first.gateway.relay.router.ChannelSelector;
import com.first.gateway.relay.router.RetryPolicy;
import com.first.gateway.relay.support.UsageParser;
import com.first.gateway.repository.UserProfileRepository;
import com.first.gateway.service.auth.ApiKeyPolicyService;
import com.first.gateway.service.auth.AuthService;
import com.first.gateway.service.billing.BillingCostCalculator;
import com.first.gateway.service.billing.BillingService;
import com.first.gateway.service.channel.ChannelRpmGuard;
import com.first.gateway.service.profile.UserMemoryService;
import com.first.gateway.service.user.UserGroupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class RelayServiceImpl implements RelayService {

    private static final Logger log = LoggerFactory.getLogger(RelayServiceImpl.class);

    private static final double DEFAULT_TEMPERATURE = 0.7;
    private static final int DEFAULT_MAX_TOKENS = 4000;
    private static final int MAX_USER_MEMORIES_IN_CHAT = 20;

    private final ChannelSelector channelSelector;
    private final OpenAiAdapter openAiAdapter;
    private final RelayUsageRecorder usageRecorder;
    private final UsageParser usageParser;
    private final ApiKeyPolicyService apiKeyPolicyService;
    private final BillingService billingService;
    private final RetryPolicy retryPolicy;
    private final UserGroupService userGroupService;
    private final ChannelHealthTracker channelHealthTracker;
    private final ChannelRpmGuard channelRpmGuard;
    private final ChatPipelineEnhancer chatPipelineEnhancer;
    private final AiServiceClient aiServiceClient;
    private final AiServiceProperties aiServiceProperties;
    private final ChannelKeyCrypto channelKeyCrypto;
    private final UserProfileRepository userProfileRepository;
    private final UserMemoryService userMemoryService;
    private final ObjectMapper objectMapper;

    public RelayServiceImpl(ChannelSelector channelSelector,
                            OpenAiAdapter openAiAdapter,
                            RelayUsageRecorder usageRecorder,
                            UsageParser usageParser,
                            ApiKeyPolicyService apiKeyPolicyService,
                            BillingService billingService,
                            RetryPolicy retryPolicy,
                            UserGroupService userGroupService,
                            ChannelHealthTracker channelHealthTracker,
                            ChannelRpmGuard channelRpmGuard,
                            @Lazy ChatPipelineEnhancer chatPipelineEnhancer,
                            AiServiceClient aiServiceClient,
                            AiServiceProperties aiServiceProperties,
                            ChannelKeyCrypto channelKeyCrypto,
                            UserProfileRepository userProfileRepository,
                            UserMemoryService userMemoryService,
                            ObjectMapper objectMapper) {
        this.channelSelector = channelSelector;
        this.openAiAdapter = openAiAdapter;
        this.usageRecorder = usageRecorder;
        this.usageParser = usageParser;
        this.apiKeyPolicyService = apiKeyPolicyService;
        this.billingService = billingService;
        this.retryPolicy = retryPolicy;
        this.userGroupService = userGroupService;
        this.channelHealthTracker = channelHealthTracker;
        this.channelRpmGuard = channelRpmGuard;
        this.chatPipelineEnhancer = chatPipelineEnhancer;
        this.aiServiceClient = aiServiceClient;
        this.aiServiceProperties = aiServiceProperties;
        this.channelKeyCrypto = channelKeyCrypto;
        this.userProfileRepository = userProfileRepository;
        this.userMemoryService = userMemoryService;
        this.objectMapper = objectMapper;
    }

    @FunctionalInterface
    private interface RelayAttempt<T> {
        T execute(ChannelSelection selection) throws Exception;
    }

    /**
     * Executes a relay attempt across candidate channels with retry, RPM guard,
     * health tracking, and failure recording. The attempt function should contain
     * only the API call and success usage recording.
     */
    private <T> T executeWithChannelFallback(
            List<ChannelSelection> candidates,
            AuthService.AuthContext auth,
            String model,
            boolean stream,
            long started,
            long tpmReserved,
            RelayAttempt<T> attemptFn) {

        if (candidates.isEmpty()) {
            throw new GatewayException(GatewayError.NO_AVAILABLE_CHANNEL);
        }

        GatewayException lastError = null;
        for (int i = 0; i < candidates.size(); i++) {
            if (i > 0 && (lastError == null || !retryPolicy.shouldRetry(i, lastError))) {
                break;
            }
            ChannelSelection sel = candidates.get(i);
            if (!channelRpmGuard.acquire(sel.channel())) {
                continue;
            }
            try {
                T result = attemptFn.execute(sel);
                channelHealthTracker.recordSuccess(sel.channel().getId());
                return result;
            } catch (GatewayException e) {
                channelHealthTracker.recordFailure(sel.channel().getId());
                lastError = e;
                if (!retryPolicy.shouldRetry(i + 1, e)) {
                    usageRecorder.recordFailure(auth, sel, model, stream, started, e, tpmReserved);
                    throw e;
                }
            }
        }
        if (lastError != null) {
            usageRecorder.recordFailure(auth, candidates.getLast(), model, stream, started, lastError, tpmReserved);
            throw lastError;
        }
        throw new GatewayException(GatewayError.NO_AVAILABLE_CHANNEL);
    }

    @Override
    public Map<String, Object> chatCompletions(AuthService.AuthContext auth,
                                                 Map<String, Object> request,
                                                 long tpmReserved) {
        long started = System.currentTimeMillis();
        String model = requireModel(request);
        Map<String, Object> enhancedRequest;
        if (aiServiceProperties.isChat() && aiServiceClient.isHealthy()) {
            enhancedRequest = request;
        } else {
            enhancedRequest = chatPipelineEnhancer.enhance(request, auth.user().getId(), auth.apiKey().getTenantId());
        }
        String enhancedModel = enhancedRequest.containsKey("model") ? enhancedRequest.get("model").toString() : model;
        apiKeyPolicyService.assertModelAllowed(auth.apiKey(), enhancedModel);
        BigDecimal groupRatio = userGroupService.ratioForUser(auth.user().getId());

        List<ChannelSelection> candidates = channelSelector.selectAllForModel(enhancedModel, auth.user().getId());
        long reserved = BillingCostCalculator.estimateReserveCost(
            enhancedRequest, candidates.getFirst().model(), groupRatio);
        billingService.preReserve(auth.apiKey().getTenantId(), reserved);

        try {
            return executeWithChannelFallback(candidates, auth, enhancedModel, false, started, tpmReserved,
                (selection) -> {
                    Map<String, Object> upstreamRequest = buildUpstreamRequest(enhancedRequest, selection.model());
                    Map<String, Object> response = tryPythonChat(auth.user().getId(), selection, upstreamRequest, false)
                        .orElseGet(() -> openAiAdapter.chat(selection.channel(), upstreamRequest));
                    UsageParser.TokenUsage usage = usageParser.fromResponse(response);
                    usageRecorder.recordSuccess(auth, selection, enhancedModel, false, started,
                        usage.promptTokens(), usage.completionTokens(), usage.totalTokens(),
                        reserved, groupRatio, tpmReserved);
                    return response;
                });
        } catch (RuntimeException ex) {
            billingService.releaseReserve(auth.apiKey().getTenantId(), reserved);
            if (ex.getCause() instanceof GatewayException ge) {
                throw ge;
            }
            throw ex;
        }
    }

    @Override
    public void chatCompletionsStream(AuthService.AuthContext auth,
                                      Map<String, Object> request,
                                      long tpmReserved,
                                      StreamConsumer consumer) {
        long started = System.currentTimeMillis();
        String model = requireModel(request);
        Map<String, Object> enhancedRequest;
        if (aiServiceProperties.isChat() && aiServiceClient.isHealthy()) {
            enhancedRequest = request;
        } else {
            enhancedRequest = chatPipelineEnhancer.enhance(request, auth.user().getId(), auth.apiKey().getTenantId());
        }
        String enhancedModel = enhancedRequest.containsKey("model") ? enhancedRequest.get("model").toString() : model;
        apiKeyPolicyService.assertModelAllowed(auth.apiKey(), enhancedModel);
        BigDecimal groupRatio = userGroupService.ratioForUser(auth.user().getId());

        List<ChannelSelection> candidates = channelSelector.selectAllForModel(enhancedModel, auth.user().getId());
        long reserved = BillingCostCalculator.estimateReserveCost(
            enhancedRequest, candidates.getFirst().model(), groupRatio);
        billingService.preReserve(auth.apiKey().getTenantId(), reserved);

        AtomicBoolean chunkSent = new AtomicBoolean(false);
        try {
            executeWithChannelFallback(candidates, auth, enhancedModel, true, started, tpmReserved,
                (selection) -> {
                    Map<String, Object> upstreamRequest = buildUpstreamRequest(enhancedRequest, selection.model());
                    StreamUsageAccumulator usageAccumulator = new StreamUsageAccumulator();
                    java.util.function.Consumer<String> chunkConsumer = chunk -> {
                        chunkSent.set(true);
                        consumer.accept(chunk);
                        UsageParser.TokenUsage chunkUsage = usageParser.fromStreamChunk(chunk);
                        if (chunkUsage.totalTokens() > 0) {
                            usageAccumulator.update(
                                chunkUsage.promptTokens(),
                                chunkUsage.completionTokens(),
                                chunkUsage.totalTokens());
                        }
                    };
                    try {
                        boolean viaPython = tryPythonChatStream(auth.user().getId(), selection, upstreamRequest, chunkConsumer);
                        if (!viaPython) {
                            openAiAdapter.chatStream(selection.channel(), upstreamRequest, chunkConsumer);
                        }
                        usageRecorder.recordSuccess(auth, selection, enhancedModel, true, started,
                            usageAccumulator.promptTokens(),
                            usageAccumulator.completionTokens(),
                            usageAccumulator.totalTokens(),
                            reserved, groupRatio, tpmReserved);
                        return null;
                    } catch (GatewayException ex) {
                        if (chunkSent.get()) {
                            usageRecorder.recordFailure(auth, selection, enhancedModel, true, started, ex, tpmReserved);
                            throw new RuntimeException(ex);
                        }
                        throw ex;
                    }
                });
        } catch (RuntimeException ex) {
            billingService.releaseReserve(auth.apiKey().getTenantId(), reserved);
            if (ex.getCause() instanceof GatewayException ge) {
                throw ge;
            }
            throw ex;
        }
    }

    @Override
    public Map<String, Object> embeddings(AuthService.AuthContext auth,
                                            Map<String, Object> request,
                                            long tpmReserved) {
        long started = System.currentTimeMillis();
        String model = requireModel(request);
        apiKeyPolicyService.assertModelAllowed(auth.apiKey(), model);
        BigDecimal groupRatio = userGroupService.ratioForUser(auth.user().getId());

        List<ChannelSelection> candidates = channelSelector.selectAllForModel(model, auth.user().getId());
        Map<String, Object> embedRequest = new HashMap<>(request);
        embedRequest.putIfAbsent("max_tokens", 1);
        long reserved = BillingCostCalculator.estimateReserveCost(
            embedRequest, candidates.getFirst().model(), groupRatio);
        billingService.preReserve(auth.apiKey().getTenantId(), reserved);

        try {
            return executeWithChannelFallback(candidates, auth, model, false, started, tpmReserved,
                (selection) -> {
                    Map<String, Object> upstreamRequest = buildUpstreamRequest(request, selection.model());
                    Map<String, Object> response;
                    if (aiServiceProperties.isEnabled() && aiServiceClient.isHealthy()) {
                        Map<String, Object> pythonRequest = new LinkedHashMap<>();
                        pythonRequest.put("input", upstreamRequest.get("input"));
                        pythonRequest.put("model", upstreamRequest.getOrDefault("model", aiServiceProperties.getEmbeddingModel()));
                        Map<String, Object> upstream = new LinkedHashMap<>();
                        upstream.put("base_url", selection.channel().getBaseUrl());
                        upstream.put("api_key", channelKeyCrypto.decrypt(selection.channel().getApiKeyEncrypted()));
                        upstream.put("model", selection.model().getModelName());
                        pythonRequest.put("upstream", upstream);
                        response = aiServiceClient.embed(pythonRequest)
                            .orElseThrow(() -> new GatewayException(GatewayError.UPSTREAM_ERROR, "ai service embed failed"));
                    } else {
                        response = openAiAdapter.embed(selection.channel(), upstreamRequest);
                    }
                    UsageParser.TokenUsage usage = usageParser.fromResponse(response);
                    usageRecorder.recordSuccess(auth, selection, model, false, started,
                        usage.promptTokens(), 0, usage.totalTokens(), reserved, groupRatio, tpmReserved);
                    return response;
                });
        } catch (RuntimeException ex) {
            billingService.releaseReserve(auth.apiKey().getTenantId(), reserved);
            if (ex.getCause() instanceof GatewayException ge) {
                throw ge;
            }
            throw ex;
        }
    }

    private Optional<Map<String, Object>> tryPythonChat(Long userId, ChannelSelection selection,
                                                          Map<String, Object> upstreamRequest, boolean stream) {
        if (!aiServiceProperties.isChat() || !aiServiceClient.isHealthy()) {
            return Optional.empty();
        }
        Map<String, Object> body = buildPythonChatBody(userId, selection, upstreamRequest, stream);
        Optional<Map<String, Object>> response = aiServiceClient.chat(body);
        if (response.isPresent()) {
            log.debug("Chat via first-ai-service for user {}", userId);
        }
        return response;
    }

    private boolean tryPythonChatStream(Long userId, ChannelSelection selection,
                                        Map<String, Object> upstreamRequest, StreamConsumer consumer) {
        if (!aiServiceProperties.isChat() || !aiServiceClient.isHealthy()) {
            return false;
        }
        Map<String, Object> body = buildPythonChatBody(userId, selection, upstreamRequest, true);
        boolean ok = aiServiceClient.chatStream(body, chunk -> consumer.accept(chunk));
        if (ok) {
            log.debug("Chat stream via first-ai-service for user {}", userId);
        }
        return ok;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> buildPythonChatBody(Long userId, ChannelSelection selection,
                                                      Map<String, Object> upstreamRequest, boolean stream) {
        Channel channel = selection.channel();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", selection.model().getModelName());
        body.put("messages", upstreamRequest.get("messages"));
        body.put("stream", stream);

        Map<String, Object> modelParams = new LinkedHashMap<>();
        if (upstreamRequest.get("temperature") instanceof Number t) {
            modelParams.put("temperature", t.doubleValue());
        } else {
            modelParams.put("temperature", DEFAULT_TEMPERATURE);
        }
        if (upstreamRequest.get("max_tokens") instanceof Number m) {
            modelParams.put("max_tokens", m.intValue());
        } else {
            modelParams.put("max_tokens", DEFAULT_MAX_TOKENS);
        }
        body.put("model_params", modelParams);

        if (upstreamRequest.get("tools") instanceof List<?> tools) {
            body.put("tools", tools);
        }

        userProfileRepository.findByUserId(userId).ifPresent(profile -> {
            Map<String, Object> profileCtx = new LinkedHashMap<>();
            profileCtx.put("ai_system_prompt", profile.getAiSystemPrompt());
            profileCtx.put("ai_tags", parseTags(profile.getAiTags()));
            body.put("user_profile", profileCtx);
        });

        List<UserMemory> memories = userMemoryService.listForUser(userId, null);
        List<Map<String, Object>> memRows = new ArrayList<>();
        int limit = Math.min(memories.size(), MAX_USER_MEMORIES_IN_CHAT);
        for (int i = 0; i < limit; i++) {
            UserMemory m = memories.get(i);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("category", m.getCategory());
            row.put("content", m.getContent());
            memRows.add(row);
        }
        body.put("user_memories", memRows);

        Map<String, Object> upstream = new LinkedHashMap<>();
        upstream.put("base_url", channel.getBaseUrl());
        upstream.put("api_key", channelKeyCrypto.decrypt(channel.getApiKeyEncrypted()));
        upstream.put("model", selection.model().getModelName());
        body.put("upstream", upstream);
        return body;
    }

    private List<String> parseTags(String aiTags) {
        if (aiTags == null || aiTags.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(aiTags, new TypeReference<>() {});
        } catch (Exception e) {
            return List.of(aiTags.trim());
        }
    }

    private Map<String, Object> buildUpstreamRequest(Map<String, Object> request,
                                                       com.first.gateway.domain.entity.ChannelModel channelModel) {
        Map<String, Object> upstream = new HashMap<>(request);
        upstream.put("model", channelModel.getModelName());
        return upstream;
    }

    private static String requireModel(Map<String, Object> request) {
        Object model = request.get("model");
        if (model == null || model.toString().isBlank()) {
            throw new GatewayException(GatewayError.INVALID_REQUEST, "model is required");
        }
        return model.toString();
    }

    static long estimateTokens(Map<String, Object> request) {
        return BillingCostCalculator.estimateTokens(request);
    }
}
