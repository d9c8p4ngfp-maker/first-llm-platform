package com.first.gateway.web.admin;

import com.first.gateway.infra.error.GatewayError;
import com.first.gateway.infra.error.GatewayException;
import com.first.gateway.infra.filter.GatewayRequestAttributes;
import com.first.gateway.infra.web.ClientIpResolver;
import com.first.gateway.service.auth.admin.AdminAuthService;
import com.first.gateway.service.auth.admin.AdminPrincipal;
import com.first.gateway.service.auth.admin.JwtService;
import com.first.gateway.web.admin.dto.LoginRequest;
import com.first.gateway.web.admin.dto.RegisterRequest;
import com.first.gateway.web.admin.support.AdminAccess;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping({"/api/v1/auth", "/admin/api/v1/auth"})
public class AdminAuthController {

    private final AdminAuthService adminAuthService;
    private final JwtService jwtService;
    private final StringRedisTemplate redisTemplate;

    public AdminAuthController(AdminAuthService adminAuthService,
                                JwtService jwtService,
                                StringRedisTemplate redisTemplate) {
        this.adminAuthService = adminAuthService;
        this.jwtService = jwtService;
        this.redisTemplate = redisTemplate;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@Valid @RequestBody LoginRequest request,
                                      HttpServletRequest httpRequest,
                                      HttpServletResponse servletResponse) {
        checkLoginRateLimit(request.username(), getClientIp(httpRequest));
        AdminAuthService.LoginResult result = adminAuthService.login(
            request.username(), request.password());
        setTokenCookie(servletResponse, result.accessToken(), result.expiresIn());
        return Map.of("user", userResponse(result.principal()));
    }

    @PostMapping("/register")
    public Map<String, Object> register(@Valid @RequestBody RegisterRequest request,
                                         HttpServletResponse servletResponse) {
        AdminAuthService.LoginResult result = adminAuthService.register(
            request.username(), request.password(), request.email());
        setTokenCookie(servletResponse, result.accessToken(), result.expiresIn());
        return Map.of("user", userResponse(result.principal()));
    }

    @GetMapping("/register-enabled")
    public Map<String, Object> registerEnabled() {
        return Map.of("enabled", adminAuthService.isRegisterEnabled());
    }

    @GetMapping("/me")
    public Map<String, Object> me(HttpServletRequest request) {
        AdminPrincipal principal = AdminAccess.requirePrincipal(currentPrincipal(request));
        return userResponse(principal);
    }

    @PostMapping("/logout")
    public Map<String, String> logout(HttpServletRequest request,
                                       HttpServletResponse servletResponse) {
        String token = extractBearer(request);
        if (token == null) {
            token = extractTokenFromCookie(request);
        }
        if (token != null) {
            jwtService.blacklist(token);
        }
        clearTokenCookie(servletResponse);
        return Map.of("message", "ok");
    }

    private void checkLoginRateLimit(String username, String ip) {
        String key = "login:limit:" + ip + ":" + username;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, Duration.ofMinutes(1));
        }
        if (count != null && count > 5) {
            throw new GatewayException(GatewayError.RATE_LIMIT_EXCEEDED, "Too many login attempts, try again later");
        }
    }

    private void setTokenCookie(HttpServletResponse response, String token, long expiresInSec) {
        jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("access_token", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge((int) expiresInSec);
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }

    private void clearTokenCookie(HttpServletResponse response) {
        jakarta.servlet.http.Cookie cookie = new jakarta.servlet.http.Cookie("access_token", "");
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
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

    private static String getClientIp(HttpServletRequest request) {
        return ClientIpResolver.resolve(request);
    }

    private static String extractBearer(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }

    private static AdminPrincipal currentPrincipal(HttpServletRequest request) {
        Object value = request.getAttribute(GatewayRequestAttributes.ADMIN_PRINCIPAL);
        if (value instanceof AdminPrincipal principal) {
            return principal;
        }
        return null;
    }

    private static Map<String, Object> userResponse(AdminPrincipal principal) {
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("id", principal.userId());
        user.put("username", principal.username());
        user.put("tenant_id", principal.tenantId());
        user.put("role", principal.role());
        return user;
    }
}
