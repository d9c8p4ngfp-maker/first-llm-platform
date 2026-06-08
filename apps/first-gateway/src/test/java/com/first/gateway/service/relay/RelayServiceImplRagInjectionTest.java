package com.first.gateway.service.relay;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.first.gateway.config.AiServiceProperties;
import com.first.gateway.config.GatewayProperties;
import com.first.gateway.domain.entity.Channel;
import com.first.gateway.domain.entity.ChannelModel;
import com.first.gateway.domain.entity.User;
import com.first.gateway.domain.entity.UserProfile;
import com.first.gateway.domain.enums.MemoryCategory;
import com.first.gateway.domain.enums.UserStatus;
import com.first.gateway.infra.ai.AiServiceClient;
import com.first.gateway.infra.ai.dto.ChatRequest;
import com.first.gateway.infra.ai.dto.RagChunkResult;
import com.first.gateway.infra.ai.dto.RagQueryResponse;
import com.first.gateway.infra.crypto.ChannelKeyCrypto;
import com.first.gateway.relay.adapter.OpenAiAdapter;
import com.first.gateway.relay.router.ChannelHealthTracker;
import com.first.gateway.relay.router.ChannelSelection;
import com.first.gateway.relay.router.ChannelSelector;
import com.first.gateway.relay.router.RetryPolicy;
import com.first.gateway.relay.support.UsageParser;
import com.first.gateway.repository.UserProfileRepository;
import com.first.gateway.service.auth.ApiKeyPolicyService;
import com.first.gateway.service.billing.BillingService;
import com.first.gateway.service.channel.ChannelRpmGuard;
import com.first.gateway.service.knowledge.KnowledgeBaseService;
import com.first.gateway.service.profile.UserMemoryService;
import com.first.gateway.service.user.UserGroupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RelayServiceImplRagInjectionTest {

    @Mock private ChannelSelector channelSelector;
    @Mock private OpenAiAdapter openAiAdapter;
    @Mock private RelayUsageRecorder usageRecorder;
    @Mock private ApiKeyPolicyService apiKeyPolicyService;
    @Mock private BillingService billingService;
    @Mock private UserGroupService userGroupService;
    @Mock private ChannelHealthTracker channelHealthTracker;
    @Mock private ChannelRpmGuard channelRpmGuard;
    @Mock private AiServiceClient aiServiceClient;
    @Mock private AiServiceProperties aiServiceProperties;
    @Mock private ChannelKeyCrypto channelKeyCrypto;
    @Mock private UserProfileRepository userProfileRepository;
    @Mock private UserMemoryService userMemoryService;
    @Mock private KnowledgeBaseService knowledgeBaseService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private RelayServiceImpl relayService;
    private Channel channel;
    private ChannelModel channelModel;

    @BeforeEach
    void setUp() {
        GatewayProperties properties = new GatewayProperties();
        properties.getRetry().setMaxAttempts(3);
        RetryPolicy retryPolicy = new RetryPolicy(properties);
        UsageParser usageParser = new UsageParser(new ObjectMapper());
        ChatPipelineEnhancer chatPipelineEnhancer = mock(ChatPipelineEnhancer.class);

        lenient().when(chatPipelineEnhancer.enhance(any(), anyLong(), anyLong()))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(aiServiceProperties.isChat()).thenReturn(false);
        lenient().when(aiServiceProperties.isEnabled()).thenReturn(false);
        lenient().when(aiServiceProperties.getEmbeddingModel()).thenReturn("text-embedding-ada-002");

        relayService = new RelayServiceImpl(
                channelSelector, openAiAdapter, usageRecorder, usageParser,
                apiKeyPolicyService, billingService, retryPolicy,
                userGroupService, channelHealthTracker, channelRpmGuard, chatPipelineEnhancer,
                aiServiceClient, aiServiceProperties, channelKeyCrypto,
                userProfileRepository, userMemoryService, knowledgeBaseService, objectMapper);

        channel = new Channel();
        channel.setId(1L);
        channel.setBaseUrl("https://api.openai.com");
        channel.setApiKeyEncrypted("enc-key");

        channelModel = new ChannelModel();
        channelModel.setId(1L);
        channelModel.setChannelId(1L);
        channelModel.setModelName("gpt-4");

        when(channelKeyCrypto.decrypt("enc-key")).thenReturn("sk-real-key");
    }

    @Test
    void extractLastUserMessage_shouldFindLastUserRole() throws Exception {
        Method method = RelayServiceImpl.class.getDeclaredMethod(
                "extractLastUserMessage", Map.class);
        method.setAccessible(true);

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(msg("system", "You are helpful"));
        messages.add(msg("user", "hello"));
        messages.add(msg("assistant", "Hi! How can I help?"));
        messages.add(msg("user", "what is X?"));

        Map<String, Object> request = Map.of("messages", messages);

        String result = (String) method.invoke(relayService, request);

        assertThat(result).isEqualTo("what is X?");
    }

    @Test
    void extractLastUserMessage_noUserMessage_shouldReturnNull() throws Exception {
        Method method = RelayServiceImpl.class.getDeclaredMethod(
                "extractLastUserMessage", Map.class);
        method.setAccessible(true);

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(msg("system", "You are helpful"));
        messages.add(msg("assistant", "Hi! How can I help?"));

        Map<String, Object> request = Map.of("messages", messages);

        String result = (String) method.invoke(relayService, request);

        assertThat(result).isNull();
    }

    @Test
    void extractLastUserMessage_emptyMessages_shouldReturnNull() throws Exception {
        Method method = RelayServiceImpl.class.getDeclaredMethod(
                "extractLastUserMessage", Map.class);
        method.setAccessible(true);

        Map<String, Object> request = Map.of("messages", List.of());

        String result = (String) method.invoke(relayService, request);

        assertThat(result).isNull();
    }

    @Test
    void extractLastUserMessage_noMessagesField_shouldReturnNull() throws Exception {
        Method method = RelayServiceImpl.class.getDeclaredMethod(
                "extractLastUserMessage", Map.class);
        method.setAccessible(true);

        Map<String, Object> request = Map.of("model", "gpt-4");

        String result = (String) method.invoke(relayService, request);

        assertThat(result).isNull();
    }

    @Test
    void buildPythonChatBody_ragContextEmpty_whenNoKbIds() throws Exception {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(msg("user", "hello"));

        Map<String, Object> upstreamRequest = new LinkedHashMap<>();
        upstreamRequest.put("model", "gpt-4");
        upstreamRequest.put("messages", messages);
        upstreamRequest.put("temperature", 0.7);
        upstreamRequest.put("max_tokens", 2000);

        ChannelSelection selection = new ChannelSelection(channel, channelModel);

        when(userProfileRepository.findByUserId(anyLong())).thenReturn(Optional.empty());
        when(userMemoryService.listRelevantForChat(anyLong(), any(), anyInt())).thenReturn(List.of());
        when(knowledgeBaseService.searchAll(eq(100L), eq("hello"), eq(5))).thenReturn(Optional.empty());

        Method method = RelayServiceImpl.class.getDeclaredMethod(
                "buildPythonChatBody", Long.class, Long.class, ChannelSelection.class,
                Map.class, boolean.class);
        method.setAccessible(true);

        ChatRequest result = (ChatRequest) method.invoke(
                relayService, 1L, 100L, selection, upstreamRequest, false);

        assertThat(result.ragContext()).isEmpty();
        verify(knowledgeBaseService).searchAll(eq(100L), eq("hello"), eq(5));
    }

    @Test
    void buildPythonChatBody_shouldCallSearchAll_whenKbIdsPresent() throws Exception {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(msg("user", "What is our company policy?"));

        Map<String, Object> upstreamRequest = new LinkedHashMap<>();
        upstreamRequest.put("model", "gpt-4");
        upstreamRequest.put("messages", messages);
        upstreamRequest.put("temperature", 0.7);
        upstreamRequest.put("max_tokens", 2000);
        upstreamRequest.put("x_knowledge_base_ids", List.of(1, 2));

        ChannelSelection selection = new ChannelSelection(channel, channelModel);

        when(userProfileRepository.findByUserId(anyLong())).thenReturn(Optional.empty());
        when(userMemoryService.listRelevantForChat(anyLong(), any(), anyInt())).thenReturn(List.of());

        RagChunkResult chunk = new RagChunkResult(10L, 1L, "Company policy states...", 0.85,
                Map.of("title", "Policy Doc"));
        when(knowledgeBaseService.searchByIds(eq(List.of(1L, 2L)), eq("What is our company policy?"), eq(5)))
                .thenReturn(Optional.of(new RagQueryResponse(List.of(chunk))));

        Method method = RelayServiceImpl.class.getDeclaredMethod(
                "buildPythonChatBody", Long.class, Long.class, ChannelSelection.class,
                Map.class, boolean.class);
        method.setAccessible(true);

        ChatRequest result = (ChatRequest) method.invoke(
                relayService, 1L, 100L, selection, upstreamRequest, false);

        assertThat(result.ragContext()).hasSize(1);
        assertThat(result.ragContext().get(0).documentId()).isEqualTo(10L);
        assertThat(result.ragContext().get(0).score()).isEqualTo(0.85);
        verify(knowledgeBaseService).searchByIds(eq(List.of(1L, 2L)), eq("What is our company policy?"), eq(5));
    }

    @Test
    void buildPythonChatBody_ragContextEmpty_whenSearchAllReturnsEmpty() throws Exception {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(msg("user", "irrelevant question"));

        Map<String, Object> upstreamRequest = new LinkedHashMap<>();
        upstreamRequest.put("model", "gpt-4");
        upstreamRequest.put("messages", messages);
        upstreamRequest.put("temperature", 0.7);
        upstreamRequest.put("max_tokens", 2000);
        upstreamRequest.put("x_knowledge_base_ids", List.of(999));

        ChannelSelection selection = new ChannelSelection(channel, channelModel);

        when(userProfileRepository.findByUserId(anyLong())).thenReturn(Optional.empty());
        when(userMemoryService.listRelevantForChat(anyLong(), any(), anyInt())).thenReturn(List.of());

        when(knowledgeBaseService.searchByIds(eq(List.of(999L)), eq("irrelevant question"), eq(5)))
                .thenReturn(Optional.empty());

        Method method = RelayServiceImpl.class.getDeclaredMethod(
                "buildPythonChatBody", Long.class, Long.class, ChannelSelection.class,
                Map.class, boolean.class);
        method.setAccessible(true);

        ChatRequest result = (ChatRequest) method.invoke(
                relayService, 1L, 100L, selection, upstreamRequest, false);

        assertThat(result.ragContext()).isEmpty();
    }

    @Test
    void buildPythonChatBody_shouldIncludeUserProfile_whenProfileExists() throws Exception {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(msg("user", "hello"));

        Map<String, Object> upstreamRequest = new LinkedHashMap<>();
        upstreamRequest.put("model", "gpt-4");
        upstreamRequest.put("messages", messages);
        upstreamRequest.put("temperature", 0.7);
        upstreamRequest.put("max_tokens", 2000);

        ChannelSelection selection = new ChannelSelection(channel, channelModel);

        UserProfile profile = new UserProfile();
        profile.setAiSystemPrompt("Be helpful and concise.");
        profile.setAiTags("[\"expert\", \"friendly\"]");
        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));
        when(userMemoryService.listRelevantForChat(anyLong(), any(), anyInt())).thenReturn(List.of());

        Method method = RelayServiceImpl.class.getDeclaredMethod(
                "buildPythonChatBody", Long.class, Long.class, ChannelSelection.class,
                Map.class, boolean.class);
        method.setAccessible(true);

        ChatRequest result = (ChatRequest) method.invoke(
                relayService, 1L, 100L, selection, upstreamRequest, false);

        assertThat(result.userProfile()).isNotNull();
        assertThat(result.userProfile().aiSystemPrompt()).isEqualTo("Be helpful and concise.");
    }

    @Test
    void buildPythonChatBody_shouldIncludeUserMemories_withLimit() throws Exception {
        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(msg("user", "hello"));

        Map<String, Object> upstreamRequest = new LinkedHashMap<>();
        upstreamRequest.put("model", "gpt-4");
        upstreamRequest.put("messages", messages);
        upstreamRequest.put("temperature", 0.7);
        upstreamRequest.put("max_tokens", 2000);

        ChannelSelection selection = new ChannelSelection(channel, channelModel);

        when(userProfileRepository.findByUserId(anyLong())).thenReturn(Optional.empty());

        // Create multiple memories
        List<com.first.gateway.domain.entity.UserMemory> memories = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            com.first.gateway.domain.entity.UserMemory mem =
                    new com.first.gateway.domain.entity.UserMemory();
            mem.setCategory(MemoryCategory.FACT);
            mem.setContent("Memory " + i);
            memories.add(mem);
        }
        when(userMemoryService.listForUser(anyLong(), any())).thenReturn(memories);

        Method method = RelayServiceImpl.class.getDeclaredMethod(
                "buildPythonChatBody", Long.class, Long.class, ChannelSelection.class,
                Map.class, boolean.class);
        method.setAccessible(true);

        ChatRequest result = (ChatRequest) method.invoke(
                relayService, 1L, 100L, selection, upstreamRequest, false);

        assertThat(result.userMemories()).hasSize(5);
    }

    private static Map<String, Object> msg(String role, String content) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("role", role);
        m.put("content", content);
        return m;
    }
}
