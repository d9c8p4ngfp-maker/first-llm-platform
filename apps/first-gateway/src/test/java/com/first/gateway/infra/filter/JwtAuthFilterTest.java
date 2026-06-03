package com.first.gateway.infra.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.first.gateway.domain.entity.User;
import com.first.gateway.domain.enums.UserStatus;
import com.first.gateway.infra.error.GatewayError;
import com.first.gateway.infra.error.GatewayException;
import com.first.gateway.service.auth.admin.AdminAuthService;
import com.first.gateway.service.auth.admin.AdminPrincipal;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock
    private AdminAuthService adminAuthService;
    @Mock
    private FilterChain filterChain;

    private JwtAuthFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthFilter(adminAuthService, new ObjectMapper());
    }

    @Test
    void shouldNotFilter_skipsLoginPath() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/admin/api/v1/auth/login");
        assertEquals(true, filter.shouldNotFilter(request));
    }

    @Test
    void shouldNotFilter_skipsWorkspaceLoginPath() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        assertEquals(true, filter.shouldNotFilter(request));
    }

    @Test
    void shouldNotFilter_doesNotSkipWorkspaceApi() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/channels");
        assertEquals(false, filter.shouldNotFilter(request));
    }

    @Test
    void shouldNotFilter_skipsV1Paths() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/v1/chat/completions");
        assertEquals(true, filter.shouldNotFilter(request));
    }

    @Test
    void doFilterInternal_setsAdminPrincipalOnSuccess() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/users");
        request.addHeader("Authorization", "Bearer jwt-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AdminPrincipal principal = new AdminPrincipal(1L, 1L, "admin", "OWNER");
        when(adminAuthService.authenticate("jwt-token")).thenReturn(principal);

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(principal, request.getAttribute(GatewayRequestAttributes.ADMIN_PRINCIPAL));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_returns401WhenJwtInvalid() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/channels");
        request.addHeader("Authorization", "Bearer bad");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(adminAuthService.authenticate("bad"))
            .thenThrow(new GatewayException(GatewayError.INVALID_JWT));

        filter.doFilterInternal(request, response, filterChain);

        assertEquals(401, response.getStatus());
        assertNotNull(response.getContentAsString());
        verify(filterChain, never()).doFilter(any(), any());
    }
}