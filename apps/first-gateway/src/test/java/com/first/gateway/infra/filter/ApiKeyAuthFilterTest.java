package com.first.gateway.infra.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.first.gateway.domain.entity.ApiKey;
import com.first.gateway.domain.entity.User;
import com.first.gateway.domain.enums.ApiKeyStatus;
import com.first.gateway.domain.enums.UserStatus;
import com.first.gateway.infra.error.GatewayError;
import com.first.gateway.infra.error.GatewayException;
import com.first.gateway.service.auth.ApiKeyPolicyService;
import com.first.gateway.service.auth.AuthService;
import com.first.gateway.service.auth.RateLimitCheckout;
import com.first.gateway.service.auth.RateLimitService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiKeyAuthFilterTest {

    @Mock
    private AuthService authService;
    @Mock
    private ApiKeyPolicyService apiKeyPolicyService;
    @Mock
    private RateLimitService rateLimitService;
    @Mock
    private FilterChain filterChain;

    private ApiKeyAuthFilter filter;

    @BeforeEach
    void setUp() {
        filter = new ApiKeyAuthFilter(
            authService, apiKeyPolicyService, rateLimitService, new ObjectMapper());
    }

    @Test
    void extractBearerToken_readsBearerPrefix() {
        assertEquals("sk-test", ApiKeyAuthFilter.extractBearerToken("Bearer sk-test"));
    }

    @Test
    void extractBearerToken_returnsNullWhenMissing() {
        assertNull(ApiKeyAuthFilter.extractBearerToken(null));
    }

    @Test
    void shouldNotFilter_skipsNonV1Paths() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/health");
        assertEquals(true, filter.shouldNotFilter(request));
    }

    @Test
    void doFilterInternal_setsAuthContextOnSuccess() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/v1/chat/completions");
        request.addHeader("Authorization", "Bearer sk-valid");
        request.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthService.AuthContext authContext = new AuthService.AuthContext(sampleApiKey(), sampleUser());
        RateLimitCheckout checkout = new RateLimitCheckout("slot-1", 0);
        when(authService.authenticateApiKey("sk-valid")).thenReturn(authContext);
        when(rateLimitService.acquire(authContext.apiKey())).thenReturn(checkout);

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(authContext, request.getAttribute(GatewayRequestAttributes.AUTH_CONTEXT));
        verify(apiKeyPolicyService).assertIpAllowed(authContext.apiKey(), "127.0.0.1");
        verify(rateLimitService).acquire(authContext.apiKey());
        verify(filterChain).doFilter(request, response);
        verify(rateLimitService).releaseConcurrency(authContext.apiKey(), checkout);
    }

    @Test
    void doFilterInternal_returns401WhenAuthFails() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/models");
        request.addHeader("Authorization", "Bearer bad");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(authService.authenticateApiKey("bad"))
            .thenThrow(new GatewayException(GatewayError.INVALID_API_KEY));

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(401, response.getStatus());
        assertNotNull(response.getContentAsString());
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void doFilterInternal_returns403WhenIpBlocked() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/v1/models");
        request.addHeader("Authorization", "Bearer sk-valid");
        request.setRemoteAddr("10.0.0.5");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AuthService.AuthContext authContext = new AuthService.AuthContext(sampleApiKey(), sampleUser());
        when(authService.authenticateApiKey("sk-valid")).thenReturn(authContext);
        doThrow(new GatewayException(GatewayError.IP_NOT_ALLOWED))
            .when(apiKeyPolicyService).assertIpAllowed(authContext.apiKey(), "10.0.0.5");

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(403, response.getStatus());
        verify(filterChain, never()).doFilter(any(), any());
    }

    private static ApiKey sampleApiKey() {
        ApiKey apiKey = new ApiKey();
        apiKey.setId(1L);
        apiKey.setTenantId(1L);
        apiKey.setUserId(1L);
        apiKey.setStatus(ApiKeyStatus.ACTIVE);
        return apiKey;
    }

    private static User sampleUser() {
        User user = new User();
        user.setId(1L);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }
}
