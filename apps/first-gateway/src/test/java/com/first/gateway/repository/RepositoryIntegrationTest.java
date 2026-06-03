package com.first.gateway.repository;

import com.first.gateway.domain.entity.BillingRecord;
import com.first.gateway.domain.entity.Quota;
import com.first.gateway.domain.entity.RedemptionCode;
import com.first.gateway.domain.entity.Tenant;
import com.first.gateway.domain.entity.TokenUsageLog;
import com.first.gateway.domain.enums.BillingRecordType;
import com.first.gateway.service.log.StatsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class RepositoryIntegrationTest {

    @Autowired
    private TenantRepository tenantRepository;
    @Autowired
    private QuotaRepository quotaRepository;
    @Autowired
    private UserGroupRepository userGroupRepository;
    @Autowired
    private BillingRecordRepository billingRecordRepository;
    @Autowired
    private RedemptionCodeRepository redemptionCodeRepository;
    @Autowired
    private TokenUsageLogRepository tokenUsageLogRepository;
    @Autowired
    private StatsService statsService;

    @Test
    void quotaConsumeAtomic_decreasesBalance() {
        long tenantId = createTenant();
        saveQuota(tenantId, 1000, 0);

        int affected = quotaRepository.consumeAtomic(tenantId, "SUBSCRIPTION", 500);

        assertEquals(1, affected);
        Quota quota = quotaRepository.findFirstByTenantIdAndTypeOrderByCreatedAtDesc(tenantId, "SUBSCRIPTION")
            .orElseThrow();
        assertEquals(500, quota.getUsedTokens());
    }

    @Test
    void quotaConsumeAtomic_rejectsInsufficientBalance() {
        long tenantId = createTenant();
        saveQuota(tenantId, 100, 90);

        int affected = quotaRepository.consumeAtomic(tenantId, "SUBSCRIPTION", 50);

        assertEquals(0, affected);
        Quota quota = quotaRepository.findFirstByTenantIdAndTypeOrderByCreatedAtDesc(tenantId, "SUBSCRIPTION")
            .orElseThrow();
        assertEquals(90, quota.getUsedTokens());
    }

    @Test
    void quotaAdjustUsed_negativeDelta() {
        long tenantId = createTenant();
        saveQuota(tenantId, 1000, 100);

        quotaRepository.adjustUsed(tenantId, "SUBSCRIPTION", -30);

        Quota quota = quotaRepository.findFirstByTenantIdAndTypeOrderByCreatedAtDesc(tenantId, "SUBSCRIPTION")
            .orElseThrow();
        assertEquals(70, quota.getUsedTokens());
    }

    @Test
    void quotaAddTotalTokens_increases() {
        long tenantId = createTenant();
        saveQuota(tenantId, 1000, 0);

        quotaRepository.addTotalTokens(tenantId, "SUBSCRIPTION", 500);

        Quota quota = quotaRepository.findFirstByTenantIdAndTypeOrderByCreatedAtDesc(tenantId, "SUBSCRIPTION")
            .orElseThrow();
        assertEquals(1500, quota.getTotalTokens());
    }

    @Test
    void userGroupSeed_exists() {
        assertTrue(userGroupRepository.findByName("default").isPresent());
        assertEquals(new BigDecimal("1.000"),
            userGroupRepository.findByName("default").orElseThrow().getRatio());
    }

    @Test
    void billingRecord_insertAndQuery() {
        long tenantId = createTenant();
        BillingRecord record = new BillingRecord();
        record.setTenantId(tenantId);
        record.setUserId(1L);
        record.setType(BillingRecordType.CONSUME.name());
        record.setAmount(-100L);
        billingRecordRepository.save(record);

        List<BillingRecord> found = billingRecordRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
        assertFalse(found.isEmpty());
        assertEquals(-100L, found.getFirst().getAmount());
    }

    @Test
    void redemptionCode_uniqueConstraint() {
        RedemptionCode first = new RedemptionCode();
        first.setCode("RC-DUP-TEST");
        first.setAmount(100L);
        redemptionCodeRepository.save(first);

        RedemptionCode duplicate = new RedemptionCode();
        duplicate.setCode("RC-DUP-TEST");
        duplicate.setAmount(200L);

        assertThrows(DataIntegrityViolationException.class,
            () -> redemptionCodeRepository.saveAndFlush(duplicate));
    }

    @Test
    void dailyStats_aggregatesCorrectly() {
        long tenantId = createTenant();
        tokenUsageLogRepository.save(log(tenantId, 10, 5, 15));
        tokenUsageLogRepository.save(log(tenantId, 20, 10, 30));
        tokenUsageLogRepository.save(log(tenantId, 5, 2, 7));

        LocalDate today = LocalDate.now();
        List<StatsService.DailyStat> stats = statsService.dailyStats(today, today, tenantId, null);

        assertEquals(1, stats.size());
        assertEquals(3, stats.getFirst().requestCount());
        assertEquals(35, stats.getFirst().promptTokens());
        assertEquals(17, stats.getFirst().completionTokens());
        assertEquals(52, stats.getFirst().totalTokens());
    }

    private long createTenant() {
        Tenant tenant = new Tenant();
        tenant.setName("repo-test-" + System.nanoTime());
        return tenantRepository.save(tenant).getId();
    }

    private void saveQuota(long tenantId, long total, long used) {
        Quota quota = new Quota();
        quota.setTenantId(tenantId);
        quota.setType("SUBSCRIPTION");
        quota.setTotalTokens(total);
        quota.setUsedTokens(used);
        quotaRepository.save(quota);
    }

    private static TokenUsageLog log(long tenantId, int prompt, int completion, int total) {
        TokenUsageLog log = new TokenUsageLog();
        log.setTenantId(tenantId);
        log.setApiKeyId(1L);
        log.setModel("deepseek-chat");
        log.setChannelId(1L);
        log.setPromptTokens(prompt);
        log.setCompletionTokens(completion);
        log.setTotalTokens(total);
        log.setStatus("SUCCESS");
        return log;
    }
}
