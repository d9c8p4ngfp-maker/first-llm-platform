package com.first.gateway.service.token;

import com.first.gateway.domain.entity.ApiKey;
import com.first.gateway.domain.enums.ApiKeyStatus;
import com.first.gateway.infra.crypto.ApiKeyHasher;
import com.first.gateway.infra.error.GatewayError;
import com.first.gateway.infra.error.GatewayException;
import com.first.gateway.repository.ApiKeyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class TokenServiceImpl implements TokenService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyHasher apiKeyHasher;

    public TokenServiceImpl(ApiKeyRepository apiKeyRepository, ApiKeyHasher apiKeyHasher) {
        this.apiKeyRepository = apiKeyRepository;
        this.apiKeyHasher = apiKeyHasher;
    }

    @Override
    public List<ApiKey> listByUserId(Long userId) {
        return apiKeyRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public Optional<ApiKey> findById(Long id) {
        return apiKeyRepository.findById(id);
    }

    @Override
    @Transactional
    public CreatedToken create(Long tenantId, Long userId, String name) {
        String rawKey = "sk-" + HexFormat.of().formatHex(randomBytes(24));
        ApiKey apiKey = new ApiKey();
        apiKey.setTenantId(tenantId);
        apiKey.setUserId(userId);
        apiKey.setKeyHash(apiKeyHasher.hash(rawKey));
        apiKey.setKeyPrefix(apiKeyHasher.prefix(rawKey));
        apiKey.setName(name);
        apiKey.setStatus(ApiKeyStatus.ACTIVE);
        return new CreatedToken(rawKey, apiKeyRepository.save(apiKey));
    }

    @Override
    @Transactional
    public ApiKey ensureActiveApiKey(Long tenantId, Long userId) {
        return apiKeyRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
            .filter(key -> key.getStatus() == ApiKeyStatus.ACTIVE)
            .findFirst()
            .orElseGet(() -> create(tenantId, userId, "console-auto").apiKey());
    }

    @Override
    @Transactional
    public void revoke(Long id) {
        ApiKey apiKey = apiKeyRepository.findById(id)
            .orElseThrow(() -> new GatewayException(GatewayError.INVALID_REQUEST, "token not found"));
        apiKey.setStatus(ApiKeyStatus.DISABLED);
        apiKeyRepository.save(apiKey);
    }

    private static byte[] randomBytes(int len) {
        byte[] bytes = new byte[len];
        RANDOM.nextBytes(bytes);
        return bytes;
    }
}
