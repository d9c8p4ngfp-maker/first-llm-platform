package com.first.gateway.service.billing;

import com.first.gateway.domain.entity.BillingRetry;
import com.first.gateway.domain.entity.Quota;
import com.first.gateway.domain.enums.BillingRecordType;
import com.first.gateway.infra.error.GatewayError;
import com.first.gateway.infra.error.GatewayException;
import com.first.gateway.repository.BillingRetryRepository;
import com.first.gateway.repository.QuotaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BillingService {

    private static final Logger log = LoggerFactory.getLogger(BillingService.class);
    private static final String QUOTA_TYPE = "SUBSCRIPTION";

    private final QuotaRepository quotaRepository;
    private final BillingRecordService billingRecordService;
    private final BillingRetryRepository billingRetryRepository;

    public BillingService(QuotaRepository quotaRepository,
                          BillingRecordService billingRecordService,
                          BillingRetryRepository billingRetryRepository) {
        this.quotaRepository = quotaRepository;
        this.billingRecordService = billingRecordService;
        this.billingRetryRepository = billingRetryRepository;
    }

    public void preReserve(Long tenantId, long estimatedTokens) {
        if (estimatedTokens <= 0) {
            return;
        }
        ensureQuotaExists(tenantId);
        if (quotaRepository.consumeAtomic(tenantId, QUOTA_TYPE, estimatedTokens) == 0) {
            throw new GatewayException(GatewayError.INSUFFICIENT_QUOTA);
        }
    }

    public void settle(Long tenantId, Long userId, long reserved, long actual, Long refLogId) {
        long delta = actual - reserved;
        if (delta != 0) {
            quotaRepository.adjustUsed(tenantId, QUOTA_TYPE, delta);
        }
        if (actual > 0) {
            billingRecordService.record(tenantId, userId, BillingRecordType.CONSUME, -actual, refLogId, null);
        }
    }

    public void releaseReserve(Long tenantId, long reserved) {
        if (reserved > 0) {
            quotaRepository.adjustUsed(tenantId, QUOTA_TYPE, -reserved);
        }
    }

    public void consume(Long tenantId, Long userId, long tokens, Long refLogId) {
        if (tokens <= 0) {
            return;
        }
        ensureQuotaExists(tenantId);
        try {
            if (quotaRepository.consumeAtomic(tenantId, QUOTA_TYPE, tokens) == 0) {
                throw new GatewayException(GatewayError.INSUFFICIENT_QUOTA);
            }
            billingRecordService.record(tenantId, userId, BillingRecordType.CONSUME, -tokens, refLogId, null);
        } catch (GatewayException ex) {
            enqueueRetry(tenantId, userId, tokens, refLogId, ex.getMessage());
            throw ex;
        } catch (RuntimeException ex) {
            enqueueRetry(tenantId, userId, tokens, refLogId, ex.getMessage());
            throw ex;
        }
    }

    public void assertSufficientQuota(Long tenantId, long estimatedTokens) {
        Quota quota = quotaRepository.findFirstByTenantIdAndTypeOrderByCreatedAtDesc(tenantId, QUOTA_TYPE)
            .orElseThrow(() -> new GatewayException(GatewayError.INSUFFICIENT_QUOTA));
        long remaining = quota.getTotalTokens() - quota.getUsedTokens();
        if (remaining < estimatedTokens) {
            throw new GatewayException(GatewayError.INSUFFICIENT_QUOTA);
        }
    }

    public Quota recharge(Long tenantId, Long userId, long tokens, BillingRecordType type, String remark) {
        ensureQuotaExists(tenantId);
        quotaRepository.addTotalTokens(tenantId, QUOTA_TYPE, tokens);
        billingRecordService.record(tenantId, userId, type, tokens, null, remark);
        return quotaRepository.findFirstByTenantIdAndTypeOrderByCreatedAtDesc(tenantId, QUOTA_TYPE)
            .orElseThrow(() -> new GatewayException(GatewayError.INTERNAL_ERROR));
    }

    public Quota adjustQuota(Long tenantId, Long userId, long deltaTokens, String remark) {
        if (deltaTokens >= 0) {
            return recharge(tenantId, userId, deltaTokens, BillingRecordType.ADJUST, remark);
        }
        ensureQuotaExists(tenantId);
        if (quotaRepository.consumeAtomic(tenantId, QUOTA_TYPE, -deltaTokens) == 0) {
            throw new GatewayException(GatewayError.INSUFFICIENT_QUOTA);
        }
        billingRecordService.record(tenantId, userId, BillingRecordType.ADJUST, deltaTokens, null, remark);
        return quotaRepository.findFirstByTenantIdAndTypeOrderByCreatedAtDesc(tenantId, QUOTA_TYPE)
            .orElseThrow(() -> new GatewayException(GatewayError.INTERNAL_ERROR));
    }

    public Quota getQuota(Long tenantId) {
        return quotaRepository.findFirstByTenantIdAndTypeOrderByCreatedAtDesc(tenantId, QUOTA_TYPE)
            .orElseThrow(() -> new GatewayException(GatewayError.INVALID_REQUEST, "quota not found"));
    }

    private void ensureQuotaExists(Long tenantId) {
        if (quotaRepository.findFirstByTenantIdAndTypeOrderByCreatedAtDesc(tenantId, QUOTA_TYPE).isEmpty()) {
            Quota quota = new Quota();
            quota.setTenantId(tenantId);
            quota.setType(QUOTA_TYPE);
            quotaRepository.save(quota);
        }
    }

    private void enqueueRetry(Long tenantId, Long userId, long amount, Long refLogId, String error) {
        BillingRetry retry = new BillingRetry();
        retry.setTenantId(tenantId);
        retry.setUserId(userId);
        retry.setAmount(amount);
        retry.setRefLogId(refLogId);
        retry.setLastError(error);
        billingRetryRepository.save(retry);
        log.warn("billing consume failed, enqueued retry tenant={} amount={}", tenantId, amount);
    }
}
