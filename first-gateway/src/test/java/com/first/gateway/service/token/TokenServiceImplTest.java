package com.first.gateway.service.token;

import com.first.gateway.domain.entity.ApiKey;
import com.first.gateway.domain.enums.ApiKeyStatus;
import com.first.gateway.infra.crypto.ApiKeyHasher;
import com.first.gateway.infra.error.GatewayError;
import com.first.gateway.infra.error.GatewayException;
import com.first.gateway.repository.ApiKeyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenServiceImplTest {

    @Mock
    private ApiKeyRepository apiKeyRepository;
    @Mock
    private ApiKeyHasher apiKeyHasher;

    private TokenServiceImpl tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenServiceImpl(apiKeyRepository, apiKeyHasher);
    }

    @Test
    void revoke_marksTokenDisabled() {
        ApiKey apiKey = new ApiKey();
        apiKey.setId(5L);
        apiKey.setStatus(ApiKeyStatus.ACTIVE);
        when(apiKeyRepository.findById(5L)).thenReturn(Optional.of(apiKey));

        tokenService.revoke(5L);

        assertEquals(ApiKeyStatus.DISABLED, apiKey.getStatus());
        verify(apiKeyRepository).save(apiKey);
    }

    @Test
    void revoke_rejectsMissingToken() {
        when(apiKeyRepository.findById(99L)).thenReturn(Optional.empty());

        GatewayException ex = assertThrows(GatewayException.class, () -> tokenService.revoke(99L));

        assertEquals(GatewayError.INVALID_REQUEST, ex.getError());
    }
}