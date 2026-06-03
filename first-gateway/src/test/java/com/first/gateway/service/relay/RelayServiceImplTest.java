package com.first.gateway.service.relay;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.first.gateway.service.relay.ChatPipelineEnhancer;
import com.first.gateway.config.GatewayProperties;
import com.first.gateway.domain.entity.ApiKey;
import com.first.gateway.domain.entity.Channel;
import com.first.gateway.domain.entity.ChannelModel;
import com.first.gateway.domain.entity.User;
import com.first.gateway.domain.enums.ApiKeyStatus;
import com.first.gateway.domain.enums.UserStatus;
import com.first.gateway.infra.error.GatewayError;
import com.first.gateway.infra.error.GatewayException;
import com.first.gateway.relay.adapter.OpenAiAdapter;
import com.first.gateway.relay.router.ChannelHealthTracker;
import com.first.gateway.relay.router.ChannelSelection;
import com.first.gateway.relay.router.ChannelSelector;
import com.first.gateway.relay.router.RetryPolicy;
import com.first.gateway.relay.support.UsageParser;
import com.first.gateway.service.auth.ApiKeyPolicyService;
import com.first.gateway.service.auth.AuthService;
import com.first.gateway.service.billing.BillingService;
import com.first.gateway.service.channel.ChannelRpmGuard;
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

    private RelayServiceImpl relayService;
    private AuthService.AuthContext authContext;
    private RetryPolicy retryPolicy;

    @BeforeEach
    void setUp() {
        GatewayProperties properties = new GatewayProperties();
        properties.getRetry().setMaxAttempts(3);
        retryPolicy = new RetryPolicy(properties);
        UsageParser usageParser = new UsageParser(new ObjectMapper());
        ChatPipelineEnhancer chatPipelineEnhancer = mock(ChatPipelineEnhancer.class);
        lenient().when(chatPipelineEnhancer.enhance(any(), anyLong(), anyLong())).thenAnswer(inv -> inv.getArgument(0));
        relayService = new RelayServiceImpl(
            channelSelector, openAiAdapter, usageRecorder, usageParser,
            apiKeyPolicyService, billingService, retryPolicy,
            userGroupService, channelHealthTracker, channelRpmGuard, chatPipelineEnhancer);
        authContext = new AuthService.AuthContext(activeApiKey(), activeUser());
        lenient().when(userGroupService.ratioForUser(1L)).thenReturn(BigDecimal.ONE);
        lenient().when(channelRpmGuard.acquire(any())).thenReturn(true);
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
        verify(billingService).preReserve(eq(1L), anyLong());
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
}
