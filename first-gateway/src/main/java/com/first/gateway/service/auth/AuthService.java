package com.first.gateway.service.auth;

import com.first.gateway.domain.entity.ApiKey;
import com.first.gateway.domain.entity.User;

public interface AuthService {

    AuthContext authenticateApiKey(String rawApiKey);

    AuthContext resolveContextForUser(Long userId, Long tenantId);

    record AuthContext(ApiKey apiKey, User user) {}
}
