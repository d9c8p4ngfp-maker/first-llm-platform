package com.first.gateway.infra.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.first.gateway.infra.error.GatewayException;
import com.first.gateway.service.auth.admin.AdminAuthService;
import com.first.gateway.service.auth.admin.AdminPrincipal;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final AdminAuthService adminAuthService;
    private final ObjectMapper objectMapper;

    public JwtAuthFilter(AdminAuthService adminAuthService, ObjectMapper objectMapper) {
        this.adminAuthService = adminAuthService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = normalizePath(request.getRequestURI());
        return !path.startsWith("/admin/") && !path.startsWith("/api/v1/");
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        // StreamingResponseBody uses async dispatch; security must be re-established.
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (isPublicAuthPath(normalizePath(request.getRequestURI()))) {
            filterChain.doFilter(request, response);
            return;
        }
        if (request.getDispatcherType() == DispatcherType.ASYNC) {
            Object stored = request.getAttribute(GatewayRequestAttributes.ADMIN_PRINCIPAL);
            if (stored instanceof AdminPrincipal principal) {
                var authentication = new UsernamePasswordAuthenticationToken(
                    principal, null, Collections.emptyList());
                SecurityContextHolder.getContext().setAuthentication(authentication);
                filterChain.doFilter(request, response);
                return;
            }
        }
        try {
            String rawJwt = ApiKeyAuthFilter.extractBearerToken(request.getHeader("Authorization"));
            if (rawJwt == null) {
                rawJwt = extractTokenFromCookie(request);
            }
            if (rawJwt == null) {
                throw new GatewayException(com.first.gateway.infra.error.GatewayError.UNAUTHORIZED, "missing token");
            }
            AdminPrincipal principal = adminAuthService.authenticate(rawJwt);
            request.setAttribute(GatewayRequestAttributes.ADMIN_PRINCIPAL, principal);
            var authentication = new UsernamePasswordAuthenticationToken(
                principal, null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (GatewayException ex) {
            FilterErrorWriter.write(objectMapper, response, ex);
        }
    }

    private static String extractTokenFromCookie(HttpServletRequest request) {
        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (jakarta.servlet.http.Cookie c : cookies) {
                if ("access_token".equals(c.getName())) {
                    return c.getValue();
                }
            }
        }
        return null;
    }

    static boolean isPublicAuthPath(String path) {
        return path.equals("/api/v1/auth/login")
            || path.equals("/api/v1/auth/register")
            || path.equals("/api/v1/auth/register-enabled")
            || path.equals("/admin/api/v1/auth/login")
            || path.equals("/admin/api/v1/auth/register")
            || path.equals("/admin/api/v1/auth/register-enabled");
    }

    static String normalizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        int query = path.indexOf('?');
        if (query >= 0) {
            path = path.substring(0, query);
        }
        if (path.length() > 1 && path.endsWith("/")) {
            return path.substring(0, path.length() - 1);
        }
        return path;
    }
}