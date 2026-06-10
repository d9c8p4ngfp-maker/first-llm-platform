package com.first.gateway.service.relay;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.first.gateway.config.AiServiceProperties;
import com.first.gateway.service.relay.ChatPipelineEnhancer;
import com.first.gateway.config.GatewayProperties;
import com.first.gateway.domain.entity.ApiKey;
import com.first.gateway.domain.entity.Channel;
import com.first.gateway.domain.entity.ChannelModel;
import com.first.gateway.domain.entity.User;
import com.first.gateway.domain.enums.ApiKeyStatus;
import com.first.gateway.domain.enums.UserStatus;
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
import com.first.gateway.service.billing.BillingService;
import com.first.gateway.service.channel.ChannelRpmGuard;
import com.first.gateway.service.knowledge.KnowledgeBaseService;
import com.first.gateway.service.profile.UserMemoryService;
import com.first.gateway.service.user.UserGroupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RelayServiceImplTest {

    @Mock
    private ChannelSelector channelSelector;
    @Mock
    private OpenAiAdapter openAiAdapter;
    @Mock
    private RelayUsageRecorder usageRecorder;
    @Mock
    private ApiKeyPolicyService apiKeyPolicyService;
    @Mock
    private BillingService billingService;
    @Mock
    private UserGroupService userGroupService;
    @Mock
    private ChannelHealthTracker channelHealthTracker;
    @Mock
    private ChannelRpmGuard channelRpmGuard;
    @Mock
    private AiServiceClient aiServiceClient;
    @Mock
    private AiServiceProperties aiServiceProperties;
    @Mock
    private ChannelKeyCrypto channelKeyCrypto;
    @Mock
    private UserProfileRepository userProfileRepository;
    @Mock
    private UserMemoryService userMemoryService;
    @Mock
    private KnowledgeBaseService knowledgeBaseService;
    @Mock
    private ObjectMapper objectMapper;

    private RelayServiceImpl relayService;
    private AuthService.AuthContext authContext;
    private RetryPolicy retryPolicy;
    private ChatPipelineEnhancer chatPipelineEnhancer;

    @BeforeEach
    void setUp() {
        GatewayProperties properties = new GatewayProperties();
        properties.getRetry().setMaxAttempts(3);
        retryPolicy = new RetryPolicy(properties);
        UsageParser usageParser = new UsageParser(new ObjectMapper());
        chatPipelineEnhancer = mock(ChatPipelineEnhancer.class);
        lenient().when(chatPipelineEnhancer.enhance(any(), anyLong(), anyLong())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(aiServiceProperties.isChat()).thenReturn(false);
        lenient().when(aiServiceProperties.isEnabled()).thenReturn(false);
        lenient().when(aiServiceProperties.getEmbeddingModel()).thenReturn("text-embedding-ada-002");
        lenient().when(aiServiceClient.isHealthy()).thenReturn(true);
        relayService = new RelayServiceImpl(
            channelSelector, openAiAdapter, usageRecorder, usageParser,
            apiKeyPolicyService, billingService, retryPolicy,
            userGroupService, channelHealthTracker, channelRpmGuard, chatPipelineEnhancer,
            aiServiceClient, aiServiceProperties, channelKeyCrypto,
            userProfileRepository, userMemoryService, knowledgeBaseService, objectMapper);
        authContext = new AuthService.AuthContext(activeApiKey(), activeUser());
        lenient().when(userGroupService.ratioForUser(1L)).thenReturn(BigDecimal.ONE);
        lenient().when(channelRpmGuard.acquire(any())).thenReturn(true);
    }

    // ---------- basic cases (unchanged logic) ----------

    private static ChannelSelection selection(String modelName, long channelId) {
        Channel channel = new Channel();
        channel.setId(channelId);
        channel.setBaseUrl("https://api.deepseek.com");
        channel.setPriority(10);
        channel.setWeight(1);
        ChannelModel model = new ChannelModel();
        model.setChannelId(channelId);
        model.setModelName(modelName);
        return new ChannelSelection(channel, model);
    }

    private static ApiKey activeApiKey() {
        ApiKey apiKey = new ApiKey();
        apiKey.setId(10L);
        apiKey.setTenantId(1L);
        apiKey.setUserId(1L);
        apiKey.setStatus(ApiKeyStatus.ACTIVE);
        return apiKey;
    }

    private static User activeUser() {
        User user = new User();
        user.setId(1L);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    @Test
    void chatCompletions_forwardsToUpstreamAndRecordsSuccess() {
        ChannelSelection selection = selection("deepseek-chat", 1L);
        Map<String, Object> request = Map.of(
            "model", "deepseek-chat",
            "messages", java.util.List.of(Map.of("role", "user", "content", "hi"))
        );
        Map<String, Object> upstreamResponse = Map.of(
            "choices", java.util.List.of(),
            "usage", Map.of("prompt_tokens", 3, "completion_tokens", 5, "total_tokens", 8)
        );

        when(channelSelector.selectAllForModel("deepseek-chat", 1L)).thenReturn(List.of(selection));
        when(openAiAdapter.chat(eq(selection.channel()), any())).thenReturn(upstreamResponse);

        Map<String, Object> result = relayService.chatCompletions(authContext, new HashMap<>(request), 0L);

        assertEquals(upstreamResponse, result);
        verify(billingService).preReserve(eq(1L), eq(1L), anyLong());
        verify(apiKeyPolicyService).assertModelAllowed(authContext.apiKey(), "deepseek-chat");
        verify(usageRecorder).recordSuccess(
            eq(authContext), eq(selection), eq("deepseek-chat"), eq(false), anyLong(),
            eq(3), eq(5), eq(8), anyLong(), eq(BigDecimal.ONE), eq(0L));
    }

    @Test
    void chatCompletions_retriesNextChannelOnUpstreamFailure() {
        ChannelSelection first = selection("deepseek-chat", 1L);
        ChannelSelection second = selection("deepseek-chat", 2L);
        GatewayException upstreamError = GatewayException.withInternal(GatewayError.UPSTREAM_ERROR, "upstream 500");
        Map<String, Object> upstreamResponse = Map.of("usage", Map.of("total_tokens", 1));

        when(channelSelector.selectAllForModel("deepseek-chat", 1L)).thenReturn(List.of(first, second));
        doThrow(upstreamError).when(openAiAdapter).chat(eq(first.channel()), any());
        when(openAiAdapter.chat(eq(second.channel()), any())).thenReturn(upstreamResponse);

        relayService.chatCompletions(authContext,
            Map.of("model", "deepseek-chat", "messages", List.of()), 0L);

        verify(openAiAdapter).chat(eq(second.channel()), any());
        verify(usageRecorder).recordSuccess(
            eq(authContext), eq(second), eq("deepseek-chat"), eq(false), anyLong(),
            eq(0), eq(0), eq(1), anyLong(), eq(BigDecimal.ONE), eq(0L));
    }

    @Test
    void chatCompletions_rewritesModelToChannelModelName() {
        ChannelSelection selection = selection("deepseek-chat", 1L);
        when(channelSelector.selectAllForModel("deepseek-chat", 1L)).thenReturn(List.of(selection));
        when(openAiAdapter.chat(eq(selection.channel()), any())).thenReturn(Map.of("usage", Map.of("total_tokens", 1)));

        relayService.chatCompletions(authContext, Map.of("model", "deepseek-chat", "messages", List.of()), 0L);

        ArgumentCaptor<Map<String, Object>> bodyCaptor = ArgumentCaptor.forClass(Map.class);
        verify(openAiAdapter).chat(eq(selection.channel()), bodyCaptor.capture());
        assertEquals("deepseek-chat", bodyCaptor.getValue().get("model"));
    }

    @Test
    void chatCompletions_requiresModel() {
        GatewayException ex = assertThrows(GatewayException.class,
            () -> relayService.chatCompletions(authContext, Map.of("messages", List.of()), 0L));

        assertEquals(GatewayError.INVALID_REQUEST, ex.getError());
    }

    @Test
    void chatCompletions_recordsFailureWhenUpstreamFails() {
        ChannelSelection selection = selection("deepseek-chat", 1L);
        GatewayException upstreamError = GatewayException.withInternal(GatewayError.UPSTREAM_ERROR, "upstream 500: boom");
        when(channelSelector.selectAllForModel("deepseek-chat", 1L)).thenReturn(List.of(selection));
        doThrow(upstreamError).when(openAiAdapter).chat(any(), any());

        GatewayException ex = assertThrows(GatewayException.class,
            () -> relayService.chatCompletions(authContext,
                Map.of("model", "deepseek-chat", "messages", List.of()), 0L));

        assertEquals(GatewayError.UPSTREAM_ERROR, ex.getError());
        verify(usageRecorder).recordFailure(
            eq(authContext), eq(selection), eq("deepseek-chat"), eq(false), anyLong(), eq(upstreamError), eq(0L));
        verify(billingService).releaseReserve(eq(1L), anyLong());
    }

    @Test
    void chatCompletionsStream_recordsSuccessWithUsageFromLastChunk() {
        ChannelSelection selection = selection("deepseek-chat", 1L);
        when(channelSelector.selectAllForModel("deepseek-chat", 1L)).thenReturn(List.of(selection));
        doAnswer(invocation -> {
            Consumer<String> consumer = invocation.getArgument(2);
            consumer.accept("data: {\"usage\":{\"prompt_tokens\":2,\"completion_tokens\":3,\"total_tokens\":5}}\n\n");
            return null;
        }).when(openAiAdapter).chatStream(any(), any(), any());

        relayService.chatCompletionsStream(authContext,
            Map.of("model", "deepseek-chat", "messages", List.of()), 0L,
            chunk -> {});

        verify(usageRecorder).recordSuccess(
            eq(authContext), eq(selection), eq("deepseek-chat"), eq(true), anyLong(),
            eq(2), eq(3), eq(5), anyLong(), eq(BigDecimal.ONE), eq(0L));
    }

    @Test
    void estimateTokens_usesMaxTokensWhenPresent() {
        assertEquals(128L, RelayServiceImpl.estimateTokens(Map.of("max_tokens", 128)));
    }

    @Test
    void estimateTokens_returnsDefaultWhenMaxTokensMissing() {
        assertEquals(2048L, RelayServiceImpl.estimateTokens(Map.of("model", "x")));
    }

    @Test
    void chatCompletionsStream_doesNotRetryAfterChunkSent() {
        ChannelSelection first = selection("deepseek-chat", 1L);
        ChannelSelection second = selection("deepseek-chat", 2L);
        GatewayException upstreamError = GatewayException.withInternal(GatewayError.UPSTREAM_ERROR, "mid-stream");

        when(channelSelector.selectAllForModel("deepseek-chat", 1L)).thenReturn(List.of(first, second));
        doAnswer(invocation -> {
            Consumer<String> consumer = invocation.getArgument(2);
            consumer.accept("data: {\"choices\":[{\"delta\":{\"content\":\"Hi\"}}]}\n\n");
            throw upstreamError;
        }).when(openAiAdapter).chatStream(eq(first.channel()), any(), any());

        GatewayException ex = assertThrows(GatewayException.class,
            () -> relayService.chatCompletionsStream(authContext,
                Map.of("model", "deepseek-chat", "messages", List.of()), 0L,
                chunk -> {}));

        assertEquals(GatewayError.UPSTREAM_ERROR, ex.getError());
        verify(openAiAdapter, never()).chatStream(eq(second.channel()), any(), any());
    }

    // ---------- edge-case tests for retry / exception / RPM ----------

    @Test
    void emptyCandidates_throwsNoAvailableChannel() {
        when(channelSelector.selectAllForModel("deepseek-chat", 1L)).thenReturn(List.of());

        GatewayException ex = assertThrows(GatewayException.class,
            () -> relayService.chatCompletions(authContext,
                Map.of("model", "deepseek-chat", "messages", List.of()), 0L));

        assertEquals(GatewayError.NO_AVAILABLE_CHANNEL, ex.getError());
    }

    @Test
    void nonRetryableGatewayException_throwsImmediatelyAndRecordsFailureForThatCandidate() {
        ChannelSelection first = selection("deepseek-chat", 1L);
        ChannelSelection second = selection("deepseek-chat", 2L);
        GatewayException invalidRequest = GatewayException.withInternal(GatewayError.INVALID_REQUEST, "bad input");

        when(channelSelector.selectAllForModel("deepseek-chat", 1L)).thenReturn(List.of(first, second));
        doThrow(invalidRequest).when(openAiAdapter).chat(eq(first.channel()), any());

        GatewayException ex = assertThrows(GatewayException.class,
            () -> relayService.chatCompletions(authContext,
                Map.of("model", "deepseek-chat", "messages", List.of()), 0L));

        assertEquals(GatewayError.INVALID_REQUEST, ex.getError());
        // Never tries second
        verify(openAiAdapter, never()).chat(eq(second.channel()), any());
        // recordFailure for the first (failing) candidate
        verify(usageRecorder).recordFailure(
            eq(authContext), eq(first), eq("deepseek-chat"), eq(false), anyLong(), eq(invalidRequest), eq(0L));
        verify(billingService).releaseReserve(eq(1L), anyLong());
    }

    @Test
    void allCandidatesExhausted_throwsLastErrorAndRecordsFailure() {
        ChannelSelection c1 = selection("deepseek-chat", 1L);
        ChannelSelection c2 = selection("deepseek-chat", 2L);
        ChannelSelection c3 = selection("deepseek-chat", 3L);
        GatewayException e1 = GatewayException.withInternal(GatewayError.UPSTREAM_ERROR, "err1");
        GatewayException e2 = GatewayException.withInternal(GatewayError.UPSTREAM_TIMEOUT, "err2");
        GatewayException e3 = GatewayException.withInternal(GatewayError.RATE_LIMIT_EXCEEDED, "err3");

        when(channelSelector.selectAllForModel("deepseek-chat", 1L)).thenReturn(List.of(c1, c2, c3));
        doThrow(e1).when(openAiAdapter).chat(eq(c1.channel()), any());
        doThrow(e2).when(openAiAdapter).chat(eq(c2.channel()), any());
        doThrow(e3).when(openAiAdapter).chat(eq(c3.channel()), any());

        // 3rd attempt (i=2): shouldRetry(3, e3) → false because attemptIndex >= maxAttempts(3)
        // throws e3 from inside the loop
        GatewayException ex = assertThrows(GatewayException.class,
            () -> relayService.chatCompletions(authContext,
                Map.of("model", "deepseek-chat", "messages", List.of()), 0L));

        assertEquals(GatewayError.RATE_LIMIT_EXCEEDED, ex.getError());
        // recordFailure for the third candidate
        verify(usageRecorder).recordFailure(
            eq(authContext), eq(c3), eq("deepseek-chat"), eq(false), anyLong(), eq(e3), eq(0L));
        verify(billingService).releaseReserve(eq(1L), anyLong());
    }

    @Test
    void rpmGuardRejectsAll_throwsNoAvailableChannel() {
        ChannelSelection c1 = selection("deepseek-chat", 1L);
        ChannelSelection c2 = selection("deepseek-chat", 2L);

        when(channelSelector.selectAllForModel("deepseek-chat", 1L)).thenReturn(List.of(c1, c2));
        when(channelRpmGuard.acquire(any())).thenReturn(false);

        GatewayException ex = assertThrows(GatewayException.class,
            () -> relayService.chatCompletions(authContext,
                Map.of("model", "deepseek-chat", "messages", List.of()), 0L));

        assertEquals(GatewayError.NO_AVAILABLE_CHANNEL, ex.getError());
        verify(billingService).preReserve(eq(1L), eq(1L), anyLong());
        verify(billingService).releaseReserve(eq(1L), anyLong());
    }

    @Test
    void rpmGuardSkipsFirstCandidate_retriesSecondSuccessfully() {
        ChannelSelection c1 = selection("deepseek-chat", 1L);
        ChannelSelection c2 = selection("deepseek-chat", 2L);
        Map<String, Object> upstreamResponse = Map.of("usage", Map.of("total_tokens", 1));

        when(channelSelector.selectAllForModel("deepseek-chat", 1L)).thenReturn(List.of(c1, c2));
        when(channelRpmGuard.acquire(c1.channel())).thenReturn(false);
        when(channelRpmGuard.acquire(c2.channel())).thenReturn(true);
        when(openAiAdapter.chat(eq(c2.channel()), any())).thenReturn(upstreamResponse);

        relayService.chatCompletions(authContext,
            Map.of("model", "deepseek-chat", "messages", List.of()), 0L);

        verify(openAiAdapter).chat(eq(c2.channel()), any());
        verify(usageRecorder).recordSuccess(
            eq(authContext), eq(c2), eq("deepseek-chat"), eq(false), anyLong(),
            eq(0), eq(0), eq(1), anyLong(), eq(BigDecimal.ONE), eq(0L));
    }

    @Test
    void channelHealthTracker_markedSuccessOnFirstCandidate() {
        ChannelSelection sel = selection("deepseek-chat", 1L);
        when(channelSelector.selectAllForModel("deepseek-chat", 1L)).thenReturn(List.of(sel));
        when(openAiAdapter.chat(eq(sel.channel()), any())).thenReturn(Map.of("usage", Map.of("total_tokens", 1)));

        relayService.chatCompletions(authContext,
            Map.of("model", "deepseek-chat", "messages", List.of()), 0L);

        verify(channelHealthTracker).recordSuccess(1L);
    }

    @Test
    void channelHealthTracker_markedFailureOnEachFailedCandidate() {
        ChannelSelection c1 = selection("deepseek-chat", 1L);
        ChannelSelection c2 = selection("deepseek-chat", 2L);
        GatewayException upstream = GatewayException.withInternal(GatewayError.UPSTREAM_ERROR, "err");

        when(channelSelector.selectAllForModel("deepseek-chat", 1L)).thenReturn(List.of(c1, c2));
        doThrow(upstream).when(openAiAdapter).chat(eq(c1.channel()), any());
        when(openAiAdapter.chat(eq(c2.channel()), any())).thenReturn(Map.of("usage", Map.of("total_tokens", 1)));

        relayService.chatCompletions(authContext,
            Map.of("model", "deepseek-chat", "messages", List.of()), 0L);

        verify(channelHealthTracker).recordFailure(1L);
        verify(channelHealthTracker).recordSuccess(2L);
    }

    @Test
    void stream_chunkSentThenGatewayException_wrapsInRuntimeException() {
        ChannelSelection first = selection("deepseek-chat", 1L);
        ChannelSelection second = selection("deepseek-chat", 2L);
        GatewayException midStreamError = GatewayException.withInternal(GatewayError.UPSTREAM_ERROR, "upstream lost");

        when(channelSelector.selectAllForModel("deepseek-chat", 1L)).thenReturn(List.of(first, second));
        doAnswer(invocation -> {
            Consumer<String> consumer = invocation.getArgument(2);
            consumer.accept("data: {\"choices\":[{\"delta\":{\"content\":\"Hi\"}}]}\n\n");
            throw midStreamError;
        }).when(openAiAdapter).chatStream(eq(first.channel()), any(), any());

        // The public method unwraps the RuntimeException and re-throws the GatewayException
        GatewayException ex = assertThrows(GatewayException.class,
            () -> relayService.chatCompletionsStream(authContext,
                Map.of("model", "deepseek-chat", "messages", List.of()), 0L,
                chunk -> {}));

        assertEquals(GatewayError.UPSTREAM_ERROR, ex.getError());
        verify(billingService).releaseReserve(eq(1L), anyLong());
    }

    @Test
    void stream_chunkNotSentThenGatewayException_canRetryNextChannel() {
        ChannelSelection first = selection("deepseek-chat", 1L);
        ChannelSelection second = selection("deepseek-chat", 2L);
        GatewayException upstreamError = GatewayException.withInternal(GatewayError.UPSTREAM_ERROR, "no chunk");

        when(channelSelector.selectAllForModel("deepseek-chat", 1L)).thenReturn(List.of(first, second));
        // No chunk emitted before the exception
        doThrow(upstreamError).when(openAiAdapter).chatStream(eq(first.channel()), any(), any());
        doAnswer(invocation -> {
            Consumer<String> consumer = invocation.getArgument(2);
            consumer.accept("data: {\"usage\":{\"total_tokens\":1}}\n\n");
            return null;
        }).when(openAiAdapter).chatStream(eq(second.channel()), any(), any());

        relayService.chatCompletionsStream(authContext,
            Map.of("model", "deepseek-chat", "messages", List.of()), 0L,
            chunk -> {});

        verify(openAiAdapter).chatStream(eq(second.channel()), any(), any());
        verify(channelHealthTracker).recordFailure(1L);
        verify(channelHealthTracker).recordSuccess(2L);
    }

    @Test
    void stream_retryBreakRespectsMaxAttempts() {
        ChannelSelection c1 = selection("deepseek-chat", 1L);
        ChannelSelection c2 = selection("deepseek-chat", 2L);
        ChannelSelection c3 = selection("deepseek-chat", 3L);
        GatewayException err = GatewayException.withInternal(GatewayError.UPSTREAM_ERROR, "fail");

        when(channelSelector.selectAllForModel("deepseek-chat", 1L)).thenReturn(List.of(c1, c2, c3));
        doThrow(err).when(openAiAdapter).chatStream(any(), any(), any());

        GatewayException ex = assertThrows(GatewayException.class,
            () -> relayService.chatCompletionsStream(authContext,
                Map.of("model", "deepseek-chat", "messages", List.of()), 0L,
                chunk -> {}));

        assertEquals(GatewayError.UPSTREAM_ERROR, ex.getError());
        // recordFailure should happen for the last candidate (c3) — the one that exhausted retries
        verify(usageRecorder).recordFailure(
            eq(authContext), eq(c3), eq("deepseek-chat"), eq(true), anyLong(), eq(err), eq(0L));
        verify(billingService).releaseReserve(eq(1L), anyLong());
    }
}
