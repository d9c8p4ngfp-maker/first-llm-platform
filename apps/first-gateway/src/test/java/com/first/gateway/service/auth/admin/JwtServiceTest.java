package com.first.gateway.service.auth.admin;

import com.first.gateway.config.AuthProperties;
import com.first.gateway.infra.error.GatewayError;
import com.first.gateway.infra.error.GatewayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @Mock
    private JwtBlacklistStore blacklistStore;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        AuthProperties properties = new AuthProperties();
        properties.setSecret("test-jwt-secret-at-least-32-bytes-long");
        properties.setExpireHours(24);
        jwtService = new JwtService(properties, blacklistStore);
    }

    @Test
    void createToken_andParseToken_roundTrip() {
        AdminPrincipal principal = new AdminPrincipal(1L, 10L, "admin", "OWNER");

        String token = jwtService.createToken(principal);
        AdminPrincipal parsed = jwtService.parseToken(token);

        assertEquals(principal, parsed);
    }

    @Test
    void parseToken_rejectsTamperedToken() {
        assertThrows(Exception.class, () -> jwtService.parseToken("invalid.token.value"));
    }

    @Test
    void isBlacklisted_redisUnavailable_throwsServiceUnavailable() {
        AdminPrincipal principal = new AdminPrincipal(1L, 10L, "admin", "PLATFORM_ADMIN");
        String token = jwtService.createToken(principal);
        doThrow(new GatewayException(GatewayError.SERVICE_UNAVAILABLE))
            .when(blacklistStore).contains(org.mockito.ArgumentMatchers.anyString());

        GatewayException ex = assertThrows(GatewayException.class, () -> jwtService.isBlacklisted(token));

        assertEquals(GatewayError.SERVICE_UNAVAILABLE, ex.getError());
    }
}