package com.first.gateway.repository;

import com.first.gateway.domain.entity.Quota;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuotaRepository extends JpaRepository<Quota, Long> {

    List<Quota> findByTenantIdOrderByCreatedAtDesc(Long tenantId);

    Optional<Quota> findFirstByTenantIdAndTypeOrderByCreatedAtDesc(Long tenantId, String type);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        UPDATE quota SET used_tokens = used_tokens + :cost
        WHERE tenant_id = :tenantId AND type = :type
        AND (total_tokens - used_tokens) >= :cost
        """, nativeQuery = true)
    int consumeAtomic(@Param("tenantId") Long tenantId,
                      @Param("type") String type,
                      @Param("cost") long cost);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        UPDATE quota SET used_tokens = used_tokens + :delta
        WHERE tenant_id = :tenantId AND type = :type
        """, nativeQuery = true)
    int adjustUsed(@Param("tenantId") Long tenantId,
                   @Param("type") String type,
                   @Param("delta") long delta);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        UPDATE quota SET total_tokens = total_tokens + :amount
        WHERE tenant_id = :tenantId AND type = :type
        """, nativeQuery = true)
    int addTotalTokens(@Param("tenantId") Long tenantId,
                       @Param("type") String type,
                       @Param("amount") long amount);
}
