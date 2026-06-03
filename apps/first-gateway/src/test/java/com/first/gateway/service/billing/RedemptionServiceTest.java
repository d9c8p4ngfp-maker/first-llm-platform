package com.first.gateway.service.billing;

import com.first.gateway.domain.entity.RedemptionCode;
import com.first.gateway.domain.enums.BillingRecordType;
import com.first.gateway.infra.error.GatewayError;
import com.first.gateway.infra.error.GatewayException;
import com.first.gateway.repository.RedemptionCodeRepository;
import com.first.gateway.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedemptionServiceTest {

    @Mock
    private RedemptionCodeRepository redemptionCodeRepository;
    @Mock
    private BillingService billingService;
    @Mock
    private UserService userService;

    private RedemptionService redemptionService;

    @BeforeEach
    void setUp() {
        redemptionService = new RedemptionService(redemptionCodeRepository, billingService, userService);
    }

    @Test
    void batchCreate_generatesCorrectCount() {
        when(redemptionCodeRepository.save(any(RedemptionCode.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        List<RedemptionCode> created = redemptionService.batchCreate(5, 10_000L, null);

        assertEquals(5, created.size());
        for (RedemptionCode code : created) {
            assertEquals(10_000L, code.getAmount());
            assertTrue(code.getCode().startsWith("RC-"));
            assertEquals(19, code.getCode().length());
        }
        verify(redemptionCodeRepository, org.mockito.Mockito.times(5)).save(any(RedemptionCode.class));
    }

    @Test
    void batchCreate_rejectsZeroCount() {
        GatewayException ex = assertThrows(GatewayException.class,
            () -> redemptionService.batchCreate(0, 100, null));
        assertEquals(GatewayError.INVALID_REQUEST, ex.getError());
    }

    @Test
    void batchCreate_rejectsNegativeAmount() {
        GatewayException ex = assertThrows(GatewayException.class,
            () -> redemptionService.batchCreate(1, -1, null));
        assertEquals(GatewayError.INVALID_REQUEST, ex.getError());
    }

    @Test
    void batchCreate_rejectsTooMany() {
        GatewayException ex = assertThrows(GatewayException.class,
            () -> redemptionService.batchCreate(1001, 100, null));
        assertEquals(GatewayError.INVALID_REQUEST, ex.getError());
    }

    @Test
    void redeem_success() {
        RedemptionCode code = unusedCode("RC-ABC", 5000L);
        when(redemptionCodeRepository.findByCode("RC-ABC")).thenReturn(Optional.of(code));
        when(userService.primaryTenantId(7L)).thenReturn(99L);
        when(redemptionCodeRepository.save(any(RedemptionCode.class))).thenAnswer(inv -> inv.getArgument(0));

        RedemptionCode redeemed = redemptionService.redeem(7L, "RC-ABC");

        verify(billingService).recharge(99L, 7L, 5000L, BillingRecordType.REDEEM, "code:RC-ABC");
        assertEquals(7L, redeemed.getUsedBy());
        assertNotNull(redeemed.getUsedAt());
    }

    @Test
    void redeem_codeNotFound() {
        when(redemptionCodeRepository.findByCode("missing")).thenReturn(Optional.empty());

        GatewayException ex = assertThrows(GatewayException.class,
            () -> redemptionService.redeem(1L, "missing"));

        assertEquals(GatewayError.INVALID_REQUEST, ex.getError());
        verify(billingService, never()).recharge(any(), any(), any(Long.class), any(), any());
    }

    @Test
    void redeem_alreadyUsed() {
        RedemptionCode code = unusedCode("RC-USED", 100L);
        code.setUsedBy(2L);
        when(redemptionCodeRepository.findByCode("RC-USED")).thenReturn(Optional.of(code));

        GatewayException ex = assertThrows(GatewayException.class,
            () -> redemptionService.redeem(1L, "RC-USED"));

        assertEquals(GatewayError.INVALID_REQUEST, ex.getError());
    }

    @Test
    void redeem_expired() {
        RedemptionCode code = unusedCode("RC-EXP", 100L);
        code.setExpiresAt(Instant.now().minus(1, ChronoUnit.DAYS));
        when(redemptionCodeRepository.findByCode("RC-EXP")).thenReturn(Optional.of(code));

        GatewayException ex = assertThrows(GatewayException.class,
            () -> redemptionService.redeem(1L, "RC-EXP"));

        assertEquals(GatewayError.INVALID_REQUEST, ex.getError());
    }

    @Test
    void redeem_notExpired() {
        RedemptionCode code = unusedCode("RC-OK", 100L);
        code.setExpiresAt(Instant.now().plus(1, ChronoUnit.DAYS));
        when(redemptionCodeRepository.findByCode("RC-OK")).thenReturn(Optional.of(code));
        when(userService.primaryTenantId(1L)).thenReturn(10L);
        when(redemptionCodeRepository.save(any(RedemptionCode.class))).thenAnswer(inv -> inv.getArgument(0));

        redemptionService.redeem(1L, "RC-OK");

        verify(billingService).recharge(eq(10L), eq(1L), eq(100L), eq(BillingRecordType.REDEEM), any());
    }

    private static RedemptionCode unusedCode(String codeValue, long amount) {
        RedemptionCode code = new RedemptionCode();
        code.setCode(codeValue);
        code.setAmount(amount);
        return code;
    }
}
