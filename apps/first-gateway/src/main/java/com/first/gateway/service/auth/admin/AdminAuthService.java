package com.first.gateway.service.auth.admin;

import com.first.gateway.domain.entity.User;
import com.first.gateway.domain.entity.UserTenantRel;
import com.first.gateway.domain.enums.UserStatus;
import com.first.gateway.infra.error.GatewayError;
import com.first.gateway.infra.error.GatewayException;
import com.first.gateway.repository.UserRepository;
import com.first.gateway.repository.UserTenantRelRepository;
import com.first.gateway.service.system.SystemConfigService;
import com.first.gateway.service.user.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class AdminAuthService {

    private final UserRepository userRepository;
    private final UserTenantRelRepository userTenantRelRepository;
    private final UserService userService;
    private final SystemConfigService systemConfigService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AdminAuthService(UserRepository userRepository,
                            UserTenantRelRepository userTenantRelRepository,
                            UserService userService,
                            SystemConfigService systemConfigService,
                            PasswordEncoder passwordEncoder,
                            JwtService jwtService) {
        this.userRepository = userRepository;
        this.userTenantRelRepository = userTenantRelRepository;
        this.userService = userService;
        this.systemConfigService = systemConfigService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public LoginResult register(String username, String password, String email) {
        if (!"true".equalsIgnoreCase(systemConfigService.getString("register_enabled", "false"))) {
            throw new GatewayException(GatewayError.ACCESS_DENIED, "registration is disabled");
        }
        userService.createUser(username, password, blankToNull(email), null);
        return login(username, password);
    }

    public boolean isRegisterEnabled() {
        return "true".equalsIgnoreCase(systemConfigService.getString("register_enabled", "false"));
    }

    @Transactional
    public LoginResult login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new GatewayException(GatewayError.INVALID_CREDENTIALS);
        }
        User user = userRepository.findByUsername(username.trim())
            .filter(u -> u.getDeleted() == 0)
            .orElseThrow(() -> new GatewayException(GatewayError.INVALID_CREDENTIALS));

        if (user.getStatus() == UserStatus.DISABLED) {
            throw new GatewayException(GatewayError.USER_BANNED);
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new GatewayException(GatewayError.INVALID_CREDENTIALS);
        }

        UserTenantRel membership = userTenantRelRepository.findFirstByUserIdOrderByJoinedAtAsc(user.getId())
            .orElseThrow(() -> new GatewayException(GatewayError.INVALID_CREDENTIALS));

        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        AdminPrincipal principal = new AdminPrincipal(
            user.getId(),
            membership.getTenantId(),
            user.getUsername(),
            membership.getRole()
        );
        return new LoginResult(
            jwtService.createToken(principal),
            jwtService.expiresInSeconds(),
            principal
        );
    }

    public AdminPrincipal authenticate(String rawJwt) {
        if (rawJwt == null || rawJwt.isBlank()) {
            throw new GatewayException(GatewayError.INVALID_JWT);
        }
        if (jwtService.isBlacklisted(rawJwt)) {
            throw new GatewayException(GatewayError.INVALID_JWT, "Token revoked");
        }
        AdminPrincipal principal = jwtService.parseToken(rawJwt);
        User user = userRepository.findById(principal.userId())
            .filter(u -> u.getDeleted() == 0 && u.getStatus() == UserStatus.ACTIVE)
            .orElseThrow(() -> new GatewayException(GatewayError.INVALID_JWT));
        // S5: reload current role from DB to prevent permission staleness
        UserTenantRel rel = userTenantRelRepository
            .findByUserIdAndTenantId(user.getId(), principal.tenantId())
            .orElseThrow(() -> new GatewayException(GatewayError.INVALID_JWT));
        return new AdminPrincipal(user.getId(), rel.getTenantId(), user.getUsername(), rel.getRole());
    }

    public record LoginResult(String accessToken, long expiresIn, AdminPrincipal principal) {}

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}