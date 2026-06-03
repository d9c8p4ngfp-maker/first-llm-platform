package com.first.gateway.service.token;

import com.first.gateway.domain.entity.ApiKey;

import java.util.List;
import java.util.Optional;

public interface TokenService {

    List<ApiKey> listByUserId(Long userId);

    Optional<ApiKey> findById(Long id);

    CreatedToken create(Long tenantId, Long userId, String name);

    ApiKey ensureActiveApiKey(Long tenantId, Long userId);

    void revoke(Long id);

    record CreatedToken(String rawKey, ApiKey apiKey) {}
}
