package com.first.gateway.service.billing;

import com.first.gateway.domain.entity.BillingRetry;
import com.first.gateway.domain.enums.BillingRecordType;
import com.first.gateway.repository.BillingRetryRepository;
import com.first.gateway.repository.QuotaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class BillingRetryScheduler {

    private static final Logger log = LoggerFactory.getLogger(BillingRetryScheduler.class);
    private static final int MAX_ATTEMPTS = 3;
    private static final String QUOTA_TYPE = "SUBSCRIPTION";

    private final BillingRetryRepository billingRetryRepository;
    private final QuotaRepository quotaRepository;
    private final BillingRecordService billingRecordService;

    public BillingRetryScheduler(BillingRetryRepository billingRetryRepository,
                                 QuotaRepository quotaRepository,
                                 BillingRecordService billingRecordService) {
        this.billingRetryRepository = billingRetryRepository;
        this.quotaRepository = quotaRepository;
        this.billingRecordService = billingRecordService;
    }

    @Scheduled(fixedDelay = 30_000)
    @Transactional
    public void processRetries() {
        List<BillingRetry> pending = billingRetryRepository.findByStatusOrderByUpdatedAtAsc("PENDING");
        for (BillingRetry item : pending) {
            if (item.getAttempt() >= MAX_ATTEMPTS) {
                item.setStatus("FAILED");
                billingRetryRepository.save(item);
                log.error("billing retry exhausted id={} tenant={} amount={}",
                    item.getId(), item.getTenantId(), item.getAmount());
                continue;
            }
            int updated = quotaRepository.consumeAtomic(item.getTenantId(), QUOTA_TYPE, item.getAmount());
            item.setAttempt(item.getAttempt() + 1);
            if (updated > 0) {
                item.setStatus("SUCCESS");
                billingRecordService.record(
                    item.getTenantId(), item.getUserId(), BillingRecordType.CONSUME,
                    -item.getAmount(), item.getRefLogId(), "retry success");
            } else if (item.getAttempt() >= MAX_ATTEMPTS) {
                item.setStatus("FAILED");
                item.setLastError("insufficient quota after retries");
            }
            billingRetryRepository.save(item);
        }
    }
}
