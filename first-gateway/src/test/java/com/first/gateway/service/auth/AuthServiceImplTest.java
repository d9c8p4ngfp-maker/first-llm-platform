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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private ApiKeyRepository apiKeyRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ApiKeyHasher apiKeyHasher;
    @Mock
    private TokenService tokenService;

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(apiKeyRepository, userRepository, apiKeyHasher, tokenService);
    }

    @Test
    void authenticateApiKey_returnsContextForValidKey() {
        ApiKey apiKey = activeApiKey();
        User user = activeUser();
        when(apiKeyHasher.hash("sk-valid")).thenReturn("hash-valid");
        when(apiKeyRepository.findByKeyHash("hash-valid")).thenReturn(Optional.of(apiKey));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        AuthService.AuthContext context = authService.authenticateApiKey("sk-valid");

        assertEquals(apiKey, context.apiKey());
        assertEquals(user, context.user());
    }

    @Test
    void authenticateApiKey_rejectsBlankKey() {
        GatewayException ex = assertThrows(GatewayException.class,
            () -> authService.authenticateApiKey("  "));

        assertEquals(GatewayError.INVALID_API_KEY, ex.getError());
    }

    @Test
    void authenticateApiKey_rejectsDisabledKey() {
        ApiKey apiKey = activeApiKey();
        apiKey.setStatus(ApiKeyStatus.DISABLED);
        when(apiKeyHasher.hash(anyString())).thenReturn("hash");
        when(apiKeyRepository.findByKeyHash("hash")).thenReturn(Optional.of(apiKey));

        GatewayException ex = assertThrows(GatewayException.class,
            () -> authService.authenticateApiKey("sk-x"));

        assertEquals(GatewayError.TOKEN_REVOKED, ex.getError());
    }

    @Test
    void authenticateApiKey_rejectsExpiredKey() {
        ApiKey apiKey = activeApiKey();
        apiKey.setExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));
        when(apiKeyHasher.hash(anyString())).thenReturn("hash");
        when(apiKeyRepository.findByKeyHash("hash")).thenReturn(Optional.of(apiKey));

        GatewayException ex = assertThrows(GatewayException.class,
            () -> authService.authenticateApiKey("sk-x"));

        assertEquals(GatewayError.TOKEN_EXPIRED, ex.getError());
    }

    @Test
    void authenticateApiKey_rejectsBannedUser() {
        ApiKey apiKey = activeApiKey();
        User user = activeUser();
        user.setStatus(UserStatus.DISABLED);
        when(apiKeyHasher.hash(anyString())).thenReturn("hash");
        when(apiKeyRepository.findByKeyHash("hash")).thenReturn(Optional.of(apiKey));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        GatewayException ex = assertThrows(GatewayException.class,
            () -> authService.authenticateApiKey("sk-x"));

        assertEquals(GatewayError.USER_BANNED, ex.getError());
    }

    @Test
    void authenticateApiKey_rejectsKeyNotFoundInDb() {
        when(apiKeyHasher.hash("sk-nonexistent")).thenReturn("hash-none");
        when(apiKeyRepository.findByKeyHash("hash-none")).thenReturn(Optional.empty());

        GatewayException ex = assertThrows(GatewayException.class,
            () -> authService.authenticateApiKey("sk-nonexistent"));

        assertEquals(GatewayError.INVALID_API_KEY, ex.getError());
    }

    @Test
    void authenticateApiKey_rejectsExhaustedKey() {
        ApiKey apiKey = activeApiKey();
        apiKey.setStatus(ApiKeyStatus.EXHAUSTED);
        when(apiKeyHasher.hash(anyString())).thenReturn("hash");
        when(apiKeyRepository.findByKeyHash("hash")).thenReturn(Optional.of(apiKey));

        GatewayException ex = assertThrows(GatewayException.class,
            () -> authService.authenticateApiKey("sk-x"));

        assertEquals(GatewayError.TOKEN_QUOTA_EXCEEDED, ex.getError());
    }

    private static ApiKey activeApiKey() {
        ApiKey apiKey = new ApiKey();
        apiKey.setId(10L);
        apiKey.setTenantId(1L);
        apiKey.setUserId(1L);
        apiKey.setStatus(ApiKeyStatus.ACTIVE);
        return apiKey;
    }

    private static User activeUser() {
        User user = new User();
        user.setId(1L);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }
}
