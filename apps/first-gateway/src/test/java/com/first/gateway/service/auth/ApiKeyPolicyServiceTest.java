package com.first.gateway.service.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.first.gateway.domain.entity.ApiKey;
import com.first.gateway.infra.error.GatewayError;
import com.first.gateway.infra.error.GatewayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ApiKeyPolicyServiceTest {

    private ApiKeyPolicyService policyService;

    @BeforeEach
    void setUp() {
        policyService = new ApiKeyPolicyService(new ObjectMapper());
    }

    @Test
    void assertModelAllowed_skipsWhenUnset() {
        ApiKey apiKey = new ApiKey();
        apiKey.setAllowedModels(null);

        policyService.assertModelAllowed(apiKey, "deepseek-chat");
    }

    @Test
    void assertModelAllowed_rejectsDisallowedModel() {
        ApiKey apiKey = new ApiKey();
        apiKey.setAllowedModels("[\"gpt-4\"]");

        GatewayException ex = assertThrows(GatewayException.class,
            () -> policyService.assertModelAllowed(apiKey, "deepseek-chat"));

        assertEquals(GatewayError.MODEL_NOT_ALLOWED, ex.getError());
    }

    @Test
    void assertModelAllowed_acceptsWhitelistedModel() {
        ApiKey apiKey = new ApiKey();
        apiKey.setAllowedModels("[\"gpt-4\",\"deepseek-chat\"]");

        policyService.assertModelAllowed(apiKey, "deepseek-chat");
    }

    @Test
    void assertModelAllowed_parsesCommaSeparatedFormat() {
        ApiKey apiKey = new ApiKey();
        apiKey.setAllowedModels("gpt-4, deepseek-chat");

        policyService.assertModelAllowed(apiKey, "deepseek-chat");

        GatewayException ex = assertThrows(GatewayException.class,
            () -> policyService.assertModelAllowed(apiKey, "claude-3"));
        assertEquals(GatewayError.MODEL_NOT_ALLOWED, ex.getError());
    }

    @Test
    void assertIpAllowed_skipsWhenNoSecurityConfig() {
        ApiKey apiKey = new ApiKey();
        apiKey.setSecurityConfig(null);

        policyService.assertIpAllowed(apiKey, "10.0.0.1");
    }

    @Test
    void assertIpAllowed_rejectsBlockedIp() {
        ApiKey apiKey = new ApiKey();
        apiKey.setSecurityConfig("{\"allowed_ips\":[\"127.0.0.1\"]}");

        GatewayException ex = assertThrows(GatewayException.class,
            () -> policyService.assertIpAllowed(apiKey, "192.168.1.10"));

        assertEquals(GatewayError.IP_NOT_ALLOWED, ex.getError());
    }

    @Test
    void assertIpAllowed_acceptsWhitelistedIp() {
        ApiKey apiKey = new ApiKey();
        apiKey.setSecurityConfig("{\"allowed_ips\":[\"127.0.0.1\"]}");

        policyService.assertIpAllowed(apiKey, "127.0.0.1");
    }
}
