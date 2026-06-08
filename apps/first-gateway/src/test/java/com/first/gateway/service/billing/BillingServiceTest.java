package com.first.gateway.service.billing;

import com.first.gateway.domain.entity.Quota;
import com.first.gateway.domain.enums.BillingRecordType;
import com.first.gateway.infra.error.GatewayError;
import com.first.gateway.infra.error.GatewayException;
import com.first.gateway.repository.BillingRetryRepository;
import com.first.gateway.repository.QuotaRepository;
import com.first.gateway.service.channel.ChannelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingServiceTest {

    @Mock
    private QuotaRepository quotaRepository;
    @Mock
    private BillingRecordService billingRecordService;
    @Mock
    private BillingRetryRepository billingRetryRepository;
    @Mock
    private ChannelService channelService;

    private BillingService billingService;

    @BeforeEach
    void setUp() {
        billingService = new BillingService(quotaRepository, billingRecordService, billingRetryRepository, channelService);
    }

    @Test
    void preReserve_passesWhenAtomicUpdateSucceeds() {
        when(quotaRepository.findFirstByTenantIdAndTypeOrderByCreatedAtDesc(1L, "SUBSCRIPTION"))
            .thenReturn(Optional.of(quota(100, 10)));
        when(quotaRepository.consumeAtomic(1L, "SUBSCRIPTION", 50)).thenReturn(1);

        billingService.preReserve(1L, 50);
    }

    @Test
    void preReserve_rejectsWhenAtomicUpdateFails() {
        when(quotaRepository.findFirstByTenantIdAndTypeOrderByCreatedAtDesc(1L, "SUBSCRIPTION"))
            .thenReturn(Optional.of(quota(100, 95)));
        when(quotaRepository.consumeAtomic(1L, "SUBSCRIPTION", 10)).thenReturn(0);

        GatewayException ex = assertThrows(GatewayException.class,
            () -> billingService.preReserve(1L, 10));

        assertEquals(GatewayError.INSUFFICIENT_QUOTA, ex.getError());
    }

    @Test
    void settle_adjustsDeltaAndRecordsBilling() {
        billingService.settle(1L, 2L, 100, 80, 99L);

        verify(quotaRepository).adjustUsed(1L, "SUBSCRIPTION", -20);
        verify(billingRecordService).record(1L, 2L, BillingRecordType.CONSUME, -80, 99L, null);
    }

    @Test
    void releaseReserve_refundsReservedAmount() {
        billingService.releaseReserve(1L, 50);

        verify(quotaRepository).adjustUsed(1L, "SUBSCRIPTION", -50);
    }

    private static Quota quota(long total, long used) {
        Quota quota = new Quota();
        quota.setTenantId(1L);
        quota.setType("SUBSCRIPTION");
        quota.setTotalTokens(total);
        quota.setUsedTokens(used);
        return quota;
    }
}
