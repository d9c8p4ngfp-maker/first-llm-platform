package com.first.gateway.service.auth.admin;

import com.first.gateway.config.AuthProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        AuthProperties properties = new AuthProperties();
        properties.setSecret("test-jwt-secret-at-least-32-bytes-long");
        properties.setExpireHours(24);
        jwtService = new JwtService(properties);
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
}