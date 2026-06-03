package com.first.gateway.service.auth.admin;

import com.first.gateway.config.AuthProperties;
import com.first.gateway.domain.entity.User;
import com.first.gateway.domain.entity.UserTenantRel;
import com.first.gateway.domain.enums.UserStatus;
import com.first.gateway.infra.error.GatewayError;
import com.first.gateway.infra.error.GatewayException;
import com.first.gateway.repository.UserRepository;
import com.first.gateway.repository.UserTenantRelRepository;
import com.first.gateway.service.system.SystemConfigService;
import com.first.gateway.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserTenantRelRepository userTenantRelRepository;
    @Mock
    private UserService userService;
    @Mock
    private SystemConfigService systemConfigService;

    private AdminAuthService adminAuthService;
    private BCryptPasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        AuthProperties properties = new AuthProperties();
        properties.setSecret("test-jwt-secret-at-least-32-bytes-long");
        properties.setExpireHours(24);
        JwtService jwtService = new JwtService(properties);
        adminAuthService = new AdminAuthService(
            userRepository, userTenantRelRepository, userService, systemConfigService, passwordEncoder, jwtService);
    }

    @Test
    void login_returnsJwtForValidCredentials() {
        User user = activeUser(passwordEncoder.encode("admin123"));
        UserTenantRel rel = membership(user.getId());
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(userTenantRelRepository.findFirstByUserIdOrderByJoinedAtAsc(1L)).thenReturn(Optional.of(rel));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminAuthService.LoginResult result = adminAuthService.login("admin", "admin123");

        assertEquals("admin", result.principal().username());
        assertEquals("OWNER", result.principal().role());
        verify(userRepository).save(user);
    }

    @Test
    void login_rejectsInvalidPassword() {
        User user = activeUser(passwordEncoder.encode("admin123"));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

        GatewayException ex = assertThrows(GatewayException.class,
            () -> adminAuthService.login("admin", "wrong"));

        assertEquals(GatewayError.INVALID_CREDENTIALS, ex.getError());
    }

    @Test
    void login_rejectsBlankCredentials() {
        GatewayException ex = assertThrows(GatewayException.class,
            () -> adminAuthService.login("", "admin123"));
        assertEquals(GatewayError.INVALID_CREDENTIALS, ex.getError());

        GatewayException ex2 = assertThrows(GatewayException.class,
            () -> adminAuthService.login("admin", ""));
        assertEquals(GatewayError.INVALID_CREDENTIALS, ex2.getError());

        GatewayException ex3 = assertThrows(GatewayException.class,
            () -> adminAuthService.login(null, null));
        assertEquals(GatewayError.INVALID_CREDENTIALS, ex3.getError());
    }

    @Test
    void login_rejectsDisabledUser() {
        User user = activeUser(passwordEncoder.encode("admin123"));
        user.setStatus(UserStatus.DISABLED);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

        GatewayException ex = assertThrows(GatewayException.class,
            () -> adminAuthService.login("admin", "admin123"));

        assertEquals(GatewayError.USER_BANNED, ex.getError());
    }

    @Test
    void login_rejectsNonexistentUser() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        GatewayException ex = assertThrows(GatewayException.class,
            () -> adminAuthService.login("ghost", "pass"));

        assertEquals(GatewayError.INVALID_CREDENTIALS, ex.getError());
    }

    private static User activeUser(String hash) {
        User user = new User();
        user.setId(1L);
        user.setUsername("admin");
        user.setPasswordHash(hash);
        user.setStatus(UserStatus.ACTIVE);
        user.setDeleted((short) 0);
        return user;
    }

    private static UserTenantRel membership(Long userId) {
        UserTenantRel rel = new UserTenantRel();
        rel.setUserId(userId);
        rel.setTenantId(1L);
        rel.setRole("OWNER");
        return rel;
    }
}