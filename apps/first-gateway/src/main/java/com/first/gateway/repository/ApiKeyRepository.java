package com.first.gateway.repository;

import com.first.gateway.domain.entity.ApiKey;
import com.first.gateway.domain.enums.ApiKeyStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    Optional<ApiKey> findByKeyHash(String keyHash);

    List<ApiKey> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<ApiKey> findByTenantIdOrderByCreatedAtDesc(Long tenantId);

    long countByUserIdAndStatus(Long userId, ApiKeyStatus status);
}
