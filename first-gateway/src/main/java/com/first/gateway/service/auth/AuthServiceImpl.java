package com.first.gateway.service.auth;

import com.first.gateway.domain.entity.ApiKey;
import com.first.gateway.domain.entity.User;
import com.first.gateway.domain.enums.ApiKeyStatus;
import com.first.gateway.domain.enums.UserStatus;
import com.first.gateway.infra.crypto.ApiKeyHasher;
import com.first.gateway.infra.error.GatewayError;
import com.first.gateway.infra.error.GatewayException;
import com.first.gateway.repository.ApiKeyRepository;
import com.first.gateway.repository.UserRepository;
import com.first.gateway.service.token.TokenService;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class AuthServiceImpl implements AuthService {

    private final ApiKeyRepository apiKeyRepository;
    private final UserRepository userRepository;
    private final ApiKeyHasher apiKeyHasher;
    private final TokenService tokenService;

    public AuthServiceImpl(ApiKeyRepository apiKeyRepository,
                           UserRepository userRepository,
                           ApiKeyHasher apiKeyHasher,
                           TokenService tokenService) {
        this.apiKeyRepository = apiKeyRepository;
        this.userRepository = userRepository;
        this.apiKeyHasher = apiKeyHasher;
        this.tokenService = tokenService;
    }

    @Override
    public AuthContext authenticateApiKey(String rawApiKey) {
        if (rawApiKey == null || rawApiKey.isBlank()) {
            throw new GatewayException(GatewayError.INVALID_API_KEY);
        }
        ApiKey apiKey = apiKeyRepository.findByKeyHash(apiKeyHasher.hash(rawApiKey))
            .orElseThrow(() -> new GatewayException(GatewayError.INVALID_API_KEY));

        if (apiKey.getStatus() == ApiKeyStatus.DISABLED) {
            throw new GatewayException(GatewayError.TOKEN_REVOKED);
        }
        if (apiKey.getStatus() == ApiKeyStatus.EXHAUSTED) {
            throw new GatewayException(GatewayError.TOKEN_QUOTA_EXCEEDED);
        }
        if (apiKey.getStatus() == ApiKeyStatus.EXPIRED) {
            throw new GatewayException(GatewayError.TOKEN_EXPIRED);
        }
        if (apiKey.getExpiresAt() != null && apiKey.getExpiresAt().isBefore(Instant.now())) {
            throw new GatewayException(GatewayError.TOKEN_EXPIRED);
        }

        User user = userRepository.findById(apiKey.getUserId())
            .orElseThrow(() -> new GatewayException(GatewayError.INVALID_API_KEY));
        if (user.getStatus() == UserStatus.DISABLED) {
            throw new GatewayException(GatewayError.USER_BANNED);
        }

        return new AuthContext(apiKey, user);
    }

    @Override
    public AuthContext resolveContextForUser(Long userId, Long tenantId) {
        ApiKey apiKey = tokenService.ensureActiveApiKey(tenantId, userId);
        User user = userRepository.findById(userId)
            .filter(u -> u.getDeleted() == 0 && u.getStatus() == UserStatus.ACTIVE)
            .orElseThrow(() -> new GatewayException(GatewayError.INVALID_API_KEY));
        return new AuthContext(apiKey, user);
    }
}
