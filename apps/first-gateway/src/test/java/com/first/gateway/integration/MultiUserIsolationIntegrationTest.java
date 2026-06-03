package com.first.gateway.integration;

import com.first.gateway.domain.entity.ChannelModel;
import com.first.gateway.domain.entity.Quota;
import com.first.gateway.domain.entity.RedemptionCode;
import com.first.gateway.domain.entity.User;
import com.first.gateway.domain.entity.UserGroup;
import com.first.gateway.domain.enums.BillingRecordType;
import com.first.gateway.domain.enums.UserStatus;
import com.first.gateway.repository.BillingRecordRepository;
import com.first.gateway.repository.QuotaRepository;
import com.first.gateway.service.billing.BillingCostCalculator;
import com.first.gateway.service.billing.BillingService;
import com.first.gateway.service.billing.RedemptionService;
import com.first.gateway.service.user.UserGroupService;
import com.first.gateway.service.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class MultiUserIsolationIntegrationTest {

    @Autowired
    private UserService userService;
    @Autowired
    private BillingService billingService;
    @Autowired
    private RedemptionService redemptionService;
    @Autowired
    private UserGroupService userGroupService;
    @Autowired
    private QuotaRepository quotaRepository;
    @Autowired
    private BillingRecordRepository billingRecordRepository;

    @Test
    void twoUsers_independentQuotas() {
        String suffix = String.valueOf(System.nanoTime());
        User userA = userService.createUser("isoA" + suffix, "pass123", "a@test.com", null);
        User userB = userService.createUser("isoB" + suffix, "pass123", "b@test.com", null);

        long tenantA = userService.primaryTenantId(userA.getId());
        long tenantB = userService.primaryTenantId(userB.getId());

        billingService.consume(tenantA, userA.getId(), 30_000, null);
        billingService.consume(tenantB, userB.getId(), 50_000, null);

        assertEquals(70_000, remaining(tenantA));
        assertEquals(50_000, remaining(tenantB));

        List<RedemptionCode> codes = redemptionService.batchCreate(1, 20_000, null);
        redemptionService.redeem(userA.getId(), codes.getFirst().getCode());

        assertEquals(90_000, remaining(tenantA));
        assertEquals(50_000, remaining(tenantB));

        assertTrue(billingRecordRepository.findByTenantIdOrderByCreatedAtDesc(tenantA).stream()
            .anyMatch(r -> BillingRecordType.REDEEM.name().equals(r.getType())));
        assertTrue(billingRecordRepository.findByTenantIdOrderByCreatedAtDesc(tenantB).stream()
            .noneMatch(r -> BillingRecordType.REDEEM.name().equals(r.getType())));
    }

    @Test
    void twoUsers_differentGroups() {
        String suffix = String.valueOf(System.nanoTime());
        UserGroup vip = userGroupService.create("vip-" + suffix, new BigDecimal("0.500"));

        User userA = userService.createUser("grpA" + suffix, "pass123", null,
            userGroupService.defaultGroup().getId());
        User userB = userService.createUser("grpB" + suffix, "pass123", null, vip.getId());

        BigDecimal ratioA = userGroupService.ratioForUser(userA.getId());
        BigDecimal ratioB = userGroupService.ratioForUser(userB.getId());

        assertEquals(0, ratioA.compareTo(BigDecimal.ONE));
        assertEquals(0, ratioB.compareTo(new BigDecimal("0.500")));

        ChannelModel model = new ChannelModel();
        model.setInputRatio(BigDecimal.ONE);
        model.setOutputRatio(new BigDecimal("2.000"));

        long costA = BillingCostCalculator.computeCost(100, 50, model, ratioA);
        long costB = BillingCostCalculator.computeCost(100, 50, model, ratioB);

        assertEquals(200, costA);
        assertEquals(100, costB);
    }

    @Test
    void disabledUser_statusUpdate() {
        String suffix = String.valueOf(System.nanoTime());
        User user = userService.createUser("dis" + suffix, "pass123", null, null);

        User disabled = userService.updateStatus(user.getId(), UserStatus.DISABLED);

        assertEquals(UserStatus.DISABLED, disabled.getStatus());
    }

    private long remaining(long tenantId) {
        Quota quota = quotaRepository.findFirstByTenantIdAndTypeOrderByCreatedAtDesc(tenantId, "SUBSCRIPTION")
            .orElseThrow();
        return quota.getTotalTokens() - quota.getUsedTokens();
    }
}
