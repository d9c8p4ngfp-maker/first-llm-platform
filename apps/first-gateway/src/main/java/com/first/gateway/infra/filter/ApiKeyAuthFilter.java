package com.first.gateway.infra.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.first.gateway.infra.error.GatewayException;
import com.first.gateway.infra.error.RateLimitExceededException;
import com.first.gateway.infra.web.ClientIpResolver;
import com.first.gateway.service.auth.ApiKeyPolicyService;
import com.first.gateway.service.auth.AuthService;
import com.first.gateway.service.auth.RateLimitCheckout;
import com.first.gateway.service.auth.RateLimitService;
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
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private final AuthService authService;
    private final ApiKeyPolicyService apiKeyPolicyService;
    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;

    public ApiKeyAuthFilter(AuthService authService,
                            ApiKeyPolicyService apiKeyPolicyService,
                            RateLimitService rateLimitService,
                            ObjectMapper objectMapper) {
        this.authService = authService;
        this.apiKeyPolicyService = apiKeyPolicyService;
        this.rateLimitService = rateLimitService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !path.startsWith("/v1/");
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (request.getDispatcherType() == DispatcherType.ASYNC) {
            Object stored = request.getAttribute(GatewayRequestAttributes.AUTH_CONTEXT);
            if (stored instanceof AuthService.AuthContext authContext) {
                var authentication = new UsernamePasswordAuthenticationToken(
                    authContext, null, Collections.emptyList());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
            filterChain.doFilter(request, response);
            return;
        }

        RateLimitCheckout checkout = null;
        AuthService.AuthContext authContext = null;
        try {
            String rawKey = extractBearerToken(request.getHeader("Authorization"));
            authContext = authService.authenticateApiKey(rawKey);
            apiKeyPolicyService.assertIpAllowed(authContext.apiKey(), ClientIpResolver.resolve(request));
            checkout = rateLimitService.acquire(authContext.apiKey());
            request.setAttribute(GatewayRequestAttributes.RATE_LIMIT_CHECKOUT, checkout);
            request.setAttribute(GatewayRequestAttributes.AUTH_CONTEXT, authContext);
            var authentication = new UsernamePasswordAuthenticationToken(
                authContext, null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (GatewayException ex) {
            if (ex instanceof RateLimitExceededException rateLimitEx) {
                rateLimitService.recordRateLimit(rateLimitEx);
            }
            FilterErrorWriter.write(objectMapper, response, ex);
        } finally {
            if (authContext != null && checkout != null) {
                rateLimitService.releaseConcurrency(authContext.apiKey(), checkout);
            }
        }
    }

    static String extractBearerToken(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return null;
        }
        if (authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return authorization.substring(7).trim();
        }
        return authorization.trim();
    }
}
