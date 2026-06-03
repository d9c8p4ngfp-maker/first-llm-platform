package com.first.gateway.service.relay;

import com.first.gateway.domain.entity.ApiKey;
import com.first.gateway.domain.entity.TokenUsageLog;
import com.first.gateway.infra.error.GatewayException;
import com.first.gateway.relay.router.ChannelSelection;
import com.first.gateway.repository.ApiKeyRepository;
import com.first.gateway.service.auth.AuthService;
import com.first.gateway.service.auth.TpmRateLimiter;
import com.first.gateway.service.billing.BillingCostCalculator;
import com.first.gateway.service.billing.BillingService;
import com.first.gateway.service.log.LogService;
import com.first.gateway.service.monitor.ApiRequestLogger;
import com.first.gateway.service.monitor.MonitoringService;
import com.first.gateway.service.notification.NotificationService;
import com.first.gateway.service.stats.RedisStatsService;
import org.slf4j.MDC;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Service
public class RelayUsageRecorder {

    private final BillingService billingService;
    private final LogService logService;
    private final ApiKeyRepository apiKeyRepository;
    private final MonitoringService monitoringService;
    private final ApiRequestLogger apiRequestLogger;
    private final TpmRateLimiter tpmRateLimiter;
    private final RedisStatsService redisStatsService;
    private final NotificationService notificationService;

    public RelayUsageRecorder(BillingService billingService,
                              LogService logService,
                              ApiKeyRepository apiKeyRepository,
                              MonitoringService monitoringService,
                              ApiRequestLogger apiRequestLogger,
                              TpmRateLimiter tpmRateLimiter,
                              @Lazy RedisStatsService redisStatsService,
                              @Lazy NotificationService notificationService) {
        this.billingService = billingService;
        this.logService = logService;
        this.apiKeyRepository = apiKeyRepository;
        this.monitoringService = monitoringService;
        this.apiRequestLogger = apiRequestLogger;
        this.tpmRateLimiter = tpmRateLimiter;
        this.redisStatsService = redisStatsService;
        this.notificationService = notificationService;
    }

    @Transactional
    public void recordSuccess(AuthService.AuthContext auth,
                              ChannelSelection selection,
                              String requestedModel,
                              boolean stream,
                              long started,
                              int promptTokens,
                              int completionTokens,
                              int totalTokens,
                              long reserved,
                              BigDecimal groupRatio,
                              long tpmReserved) {
        long latencyMs = System.currentTimeMillis() - started;
        long actual = BillingCostCalculator.computeCost(
            promptTokens, completionTokens, selection.model(), groupRatio);
        TokenUsageLog log = saveLog(auth, selection, requestedModel, stream, started,
            promptTokens, completionTokens, totalTokens, "SUCCESS", null);
        billingService.settle(
            auth.apiKey().getTenantId(), auth.user().getId(), reserved, actual, log.getId());
        touchApiKey(auth.apiKey(), actual);
        tpmRateLimiter.refund(auth.apiKey(), tpmReserved, totalTokens);
        recordMetrics(auth, selection, requestedModel, promptTokens, completionTokens, totalTokens,
            latencyMs, "success");

        if (redisStatsService != null) {
            redisStatsService.incrementAfterChat(auth.user().getId(), totalTokens, 0);
        }

        if (notificationService != null) {
            try {
                notificationService.publishStatsUpdate(auth.user().getId(), 0, totalTokens, 0);
            } catch (Exception ignored) {}
        }
    }

    @Transactional
    public void recordFailure(AuthService.AuthContext auth,
                              ChannelSelection selection,
                              String requestedModel,
                              boolean stream,
                              long started,
                              GatewayException ex,
                              long tpmReserved) {
        long latencyMs = System.currentTimeMillis() - started;
        saveLog(auth, selection, requestedModel, stream, started, 0, 0, 0, "FAILED", failureMessage(ex));
        tpmRateLimiter.refund(auth.apiKey(), tpmReserved, 0);
        recordMetrics(auth, selection, requestedModel, 0, 0, 0, latencyMs, "failed");
    }

    private void recordMetrics(AuthService.AuthContext auth,
                               ChannelSelection selection,
                               String requestedModel,
                               int promptTokens,
                               int completionTokens,
                               int totalTokens,
                               long latencyMs,
                               String status) {
        String channelTag = selection.channel().getName() != null
            ? selection.channel().getName()
            : String.valueOf(selection.channel().getId());
        monitoringService.recordRequest(requestedModel, status, latencyMs);
        monitoringService.recordChannelRequest(channelTag, status);
        monitoringService.recordChannelLatency(channelTag, latencyMs);
        if (promptTokens > 0) {
            monitoringService.recordTokens("prompt", promptTokens);
        }
        if (completionTokens > 0) {
            monitoringService.recordTokens("completion", completionTokens);
        }
        apiRequestLogger.log(
            MDC.get("traceId"),
            requestedModel,
            auth.apiKey().getKeyPrefix(),
            promptTokens,
            completionTokens,
            totalTokens,
            latencyMs,
            status,
            null);
    }

    private static String failureMessage(GatewayException ex) {
        if (ex.getInternalDetail() != null) {
            return ex.getInternalDetail();
        }
        return ex.getDetail() != null ? ex.getDetail() : ex.getError().getDefaultMessage();
    }

    private void touchApiKey(ApiKey apiKey, long tokens) {
        apiKey.setLastUsedAt(Instant.now());
        if (tokens > 0) {
            apiKey.setUsedQuota(apiKey.getUsedQuota() + tokens);
        }
        apiKeyRepository.save(apiKey);
    }

    private TokenUsageLog saveLog(AuthService.AuthContext auth,
                                  ChannelSelection selection,
                                  String requestedModel,
                                  boolean stream,
                                  long started,
                                  int promptTokens,
                                  int completionTokens,
                                  int totalTokens,
                                  String status,
                                  String errorMsg) {
        TokenUsageLog log = new TokenUsageLog();
        log.setTenantId(auth.apiKey().getTenantId());
        log.setApiKeyId(auth.apiKey().getId());
        log.setModel(requestedModel);
        log.setChannelId(selection.channel().getId());
        log.setPromptTokens(promptTokens);
        log.setCompletionTokens(completionTokens);
        log.setTotalTokens(totalTokens);
        log.setCostRatio(selection.model().getInputRatio() != null
            ? selection.model().getInputRatio() : BigDecimal.ONE);
        log.setIsStream((short) (stream ? 1 : 0));
        log.setLatencyMs((int) Math.min(Integer.MAX_VALUE, System.currentTimeMillis() - started));
        log.setStatus(status);
        log.setErrorMsg(errorMsg);
        log.setRequestId(MDC.get("traceId"));
        return logService.save(log);
    }
}
