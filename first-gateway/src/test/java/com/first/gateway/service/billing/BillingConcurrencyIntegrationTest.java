package com.first.gateway.service.billing;

import com.first.gateway.domain.entity.Quota;
import com.first.gateway.domain.entity.Tenant;
import com.first.gateway.repository.QuotaRepository;
import com.first.gateway.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("dev")
class BillingConcurrencyIntegrationTest {

    @Autowired
    private TenantRepository tenantRepository;
    @Autowired
    private QuotaRepository quotaRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;
    private long tenantId;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        tenantId = transactionTemplate.execute(status -> {
            Tenant tenant = new Tenant();
            tenant.setName("concurrency-" + System.nanoTime());
            return tenantRepository.saveAndFlush(tenant).getId();
        });
    }

    @Test
    void concurrentConsume_exact() throws Exception {
        seedQuota(10_000, 0);

        AtomicInteger successCount = runConcurrentDeductions(10, 1000);

        assertEquals(10, successCount.get());
        assertEquals(10_000L, usedTokens());
    }

    @Test
    void concurrentConsume_partialSuccess() throws Exception {
        seedQuota(10_000, 0);

        AtomicInteger successCount = runConcurrentDeductions(15, 1000);

        assertEquals(10, successCount.get());
        assertEquals(10_000L, usedTokens());
    }

    @Test
    void concurrentPreReserveAndSettle() throws Exception {
        seedQuota(50_000, 0);

        ExecutorService pool = Executors.newFixedThreadPool(5);
        CountDownLatch latch = new CountDownLatch(1);
        List<Future<Void>> futures = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            futures.add(pool.submit((Callable<Void>) () -> {
                latch.await();
                transactionTemplate.executeWithoutResult(status -> {
                    quotaRepository.consumeAtomic(tenantId, "SUBSCRIPTION", 2000);
                    quotaRepository.adjustUsed(tenantId, "SUBSCRIPTION", -500);
                });
                return null;
            }));
        }
        latch.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS));
        for (Future<Void> future : futures) {
            future.get();
        }
        assertEquals(7_500L, usedTokens());
    }

    private AtomicInteger runConcurrentDeductions(int threads, long cost) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger();
        List<Future<Void>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit((Callable<Void>) () -> {
                latch.await();
                Integer affected = transactionTemplate.execute(
                    status -> quotaRepository.consumeAtomic(tenantId, "SUBSCRIPTION", cost));
                if (affected != null && affected > 0) {
                    successCount.incrementAndGet();
                }
                return null;
            }));
        }
        latch.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS));
        for (Future<Void> future : futures) {
            future.get();
        }
        return successCount;
    }

    private void seedQuota(long total, long used) {
        transactionTemplate.executeWithoutResult(status -> {
            Quota quota = new Quota();
            quota.setTenantId(tenantId);
            quota.setType("SUBSCRIPTION");
            quota.setTotalTokens(total);
            quota.setUsedTokens(used);
            quotaRepository.saveAndFlush(quota);
        });
    }

    private long usedTokens() {
        return transactionTemplate.execute(status ->
            quotaRepository.findFirstByTenantIdAndTypeOrderByCreatedAtDesc(tenantId, "SUBSCRIPTION")
                .orElseThrow()
                .getUsedTokens());
    }
}
