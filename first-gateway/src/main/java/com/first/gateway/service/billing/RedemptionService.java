package com.first.gateway.service.billing;

import com.first.gateway.domain.entity.RedemptionCode;
import com.first.gateway.domain.enums.BillingRecordType;
import com.first.gateway.infra.error.GatewayError;
import com.first.gateway.infra.error.GatewayException;
import com.first.gateway.repository.RedemptionCodeRepository;
import com.first.gateway.service.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

@Service
public class RedemptionService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final RedemptionCodeRepository redemptionCodeRepository;
    private final BillingService billingService;
    private final UserService userService;

    public RedemptionService(RedemptionCodeRepository redemptionCodeRepository,
                             BillingService billingService,
                             UserService userService) {
        this.redemptionCodeRepository = redemptionCodeRepository;
        this.billingService = billingService;
        this.userService = userService;
    }

    public List<RedemptionCode> listAll() {
        return redemptionCodeRepository.findAll();
    }

    @Transactional
    public List<RedemptionCode> batchCreate(int count, long amount, Instant expiresAt) {
        if (count <= 0 || count > 1000) {
            throw new GatewayException(GatewayError.INVALID_REQUEST, "count must be 1-1000");
        }
        if (amount <= 0) {
            throw new GatewayException(GatewayError.INVALID_REQUEST, "amount must be positive");
        }
        List<RedemptionCode> created = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            RedemptionCode code = new RedemptionCode();
            code.setCode(generateCode());
            code.setAmount(amount);
            code.setExpiresAt(expiresAt);
            created.add(redemptionCodeRepository.save(code));
        }
        return created;
    }

    @Transactional
    public RedemptionCode redeem(Long userId, String rawCode) {
        RedemptionCode code = redemptionCodeRepository.findByCode(rawCode.trim())
            .orElseThrow(() -> new GatewayException(GatewayError.INVALID_REQUEST, "invalid redemption code"));
        if (code.getUsedBy() != null) {
            throw new GatewayException(GatewayError.INVALID_REQUEST, "code already used");
        }
        if (code.getExpiresAt() != null && code.getExpiresAt().isBefore(Instant.now())) {
            throw new GatewayException(GatewayError.INVALID_REQUEST, "code expired");
        }
        Long tenantId = userService.primaryTenantId(userId);
        billingService.recharge(tenantId, userId, code.getAmount(), BillingRecordType.REDEEM, "code:" + code.getCode());
        code.setUsedBy(userId);
        code.setUsedAt(Instant.now());
        return redemptionCodeRepository.save(code);
    }

    private static String generateCode() {
        return "RC-" + HexFormat.of().formatHex(randomBytes(8)).toUpperCase();
    }

    private static byte[] randomBytes(int len) {
        byte[] bytes = new byte[len];
        RANDOM.nextBytes(bytes);
        return bytes;
    }
}
