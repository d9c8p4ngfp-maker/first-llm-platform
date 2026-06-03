package com.first.gateway.service.billing;

import com.first.gateway.domain.entity.BillingRetry;
import com.first.gateway.domain.enums.BillingRecordType;
import com.first.gateway.repository.BillingRetryRepository;
import com.first.gateway.repository.QuotaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingRetrySchedulerTest {

    @Mock
    private BillingRetryRepository billingRetryRepository;
    @Mock
    private QuotaRepository quotaRepository;
    @Mock
    private BillingRecordService billingRecordService;

    private BillingRetryScheduler billingRetryScheduler;

    @BeforeEach
    void setUp() {
        billingRetryScheduler = new BillingRetryScheduler(
            billingRetryRepository, quotaRepository, billingRecordService);
    }

    @Test
    void processRetries_successOnFirstAttempt() {
        BillingRetry item = pending(1L, 10L, 20L, 100L, 0);
        when(billingRetryRepository.findByStatusOrderByUpdatedAtAsc("PENDING"))
            .thenReturn(List.of(item));
        when(quotaRepository.consumeAtomic(10L, "SUBSCRIPTION", 100L)).thenReturn(1);
        when(billingRetryRepository.save(any(BillingRetry.class))).thenAnswer(inv -> inv.getArgument(0));

        billingRetryScheduler.processRetries();

        assertEquals("SUCCESS", item.getStatus());
        assertEquals(1, item.getAttempt());
        verify(billingRecordService).record(
            10L, 20L, BillingRecordType.CONSUME, -100L, 1L, "retry success");
    }

    @Test
    void processRetries_failsAndIncrements() {
        BillingRetry item = pending(2L, 10L, 20L, 50L, 1);
        when(billingRetryRepository.findByStatusOrderByUpdatedAtAsc("PENDING"))
            .thenReturn(List.of(item));
        when(quotaRepository.consumeAtomic(10L, "SUBSCRIPTION", 50L)).thenReturn(0);
        when(billingRetryRepository.save(any(BillingRetry.class))).thenAnswer(inv -> inv.getArgument(0));

        billingRetryScheduler.processRetries();

        assertEquals("PENDING", item.getStatus());
        assertEquals(2, item.getAttempt());
        verify(billingRecordService, never()).record(any(), any(), any(), any(Long.class), any(), any());
    }

    @Test
    void processRetries_exhaustedAfterMaxAttempts() {
        BillingRetry item = pending(3L, 10L, 20L, 50L, 3);
        when(billingRetryRepository.findByStatusOrderByUpdatedAtAsc("PENDING"))
            .thenReturn(List.of(item));
        when(billingRetryRepository.save(any(BillingRetry.class))).thenAnswer(inv -> inv.getArgument(0));

        billingRetryScheduler.processRetries();

        assertEquals("FAILED", item.getStatus());
        verify(quotaRepository, never()).consumeAtomic(any(), any(), any(Long.class));
    }

    @Test
    void processRetries_emptyQueue() {
        when(billingRetryRepository.findByStatusOrderByUpdatedAtAsc("PENDING"))
            .thenReturn(Collections.emptyList());

        billingRetryScheduler.processRetries();

        verify(billingRetryRepository, never()).save(any());
    }

    private static BillingRetry pending(Long id, Long tenantId, Long userId, long amount, int attempt) {
        BillingRetry retry = new BillingRetry();
        retry.setId(id);
        retry.setTenantId(tenantId);
        retry.setUserId(userId);
        retry.setAmount(amount);
        retry.setAttempt(attempt);
        retry.setStatus("PENDING");
        retry.setRefLogId(1L);
        return retry;
    }
}
