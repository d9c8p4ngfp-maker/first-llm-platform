package com.first.gateway.service.user;

import com.first.gateway.domain.entity.Quota;
import com.first.gateway.domain.entity.Tenant;
import com.first.gateway.domain.entity.User;
import com.first.gateway.domain.entity.UserGroup;
import com.first.gateway.domain.entity.UserTenantRel;
import com.first.gateway.domain.enums.UserStatus;
import com.first.gateway.infra.error.GatewayError;
import com.first.gateway.infra.error.GatewayException;
import com.first.gateway.repository.QuotaRepository;
import com.first.gateway.repository.TenantRepository;
import com.first.gateway.repository.UserRepository;
import com.first.gateway.repository.UserTenantRelRepository;
import com.first.gateway.service.billing.BillingService;
import com.first.gateway.service.system.SystemConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private TenantRepository tenantRepository;
    @Mock
    private UserTenantRelRepository userTenantRelRepository;
    @Mock
    private QuotaRepository quotaRepository;
    @Mock
    private UserGroupService userGroupService;
    @Mock
    private SystemConfigService systemConfigService;
    @Mock
    private BillingService billingService;
    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(
            userRepository, tenantRepository, userTenantRelRepository, quotaRepository,
            userGroupService, systemConfigService, billingService, passwordEncoder);
    }

    @Test
    void createUser_fullAtomicChain() {
        UserGroup group = group(2L);
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userGroupService.findById(2L)).thenReturn(Optional.of(group));
        when(passwordEncoder.encode("secret1234567")).thenReturn("encoded");
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> {
            Tenant t = inv.getArgument(0);
            t.setId(100L);
            return t;
        });
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(200L);
            return u;
        });
        when(systemConfigService.getLong("quota_for_new_user", 100_000L)).thenReturn(100_000L);
        when(quotaRepository.save(any(Quota.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.createUser("alice", "secret1234567", "a@test.com", 2L);

        verify(tenantRepository).save(any(Tenant.class));
        verify(userRepository).save(any(User.class));
        verify(userTenantRelRepository).save(any(UserTenantRel.class));
        verify(quotaRepository).save(any(Quota.class));
        verify(passwordEncoder).encode("secret1234567");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("encoded", userCaptor.getValue().getPasswordHash());
        assertEquals(2L, userCaptor.getValue().getGroupId());
    }

    @Test
    void createUser_defaultGroup() {
        UserGroup group = group(1L);
        when(userRepository.existsByUsername("bob")).thenReturn(false);
        when(userGroupService.defaultGroup()).thenReturn(group);
        when(passwordEncoder.encode("secret1234567")).thenReturn("encoded");
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(inv -> {
            Tenant t = inv.getArgument(0);
            t.setId(101L);
            return t;
        });
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(201L);
            return u;
        });
        when(systemConfigService.getLong("quota_for_new_user", 100_000L)).thenReturn(50_000L);
        when(quotaRepository.save(any(Quota.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.createUser("bob", "secret2", null, null);

        verify(userGroupService).defaultGroup();
    }

    @Test
    void createUser_shortPassword() {
        GatewayException ex = assertThrows(GatewayException.class,
            () -> userService.createUser("x", "12345", null, null));
        assertEquals(GatewayError.INVALID_REQUEST, ex.getError());
    }

    @Test
    void createUser_duplicateUsername() {
        when(userRepository.existsByUsername("dup")).thenReturn(true);

        GatewayException ex = assertThrows(GatewayException.class,
            () -> userService.createUser("dup", "secret1234567", null, null));
        assertEquals(GatewayError.INVALID_REQUEST, ex.getError());
    }

    @Test
    void createUser_invalidGroup() {
        when(userRepository.existsByUsername("carol")).thenReturn(false);
        when(userGroupService.findById(999L)).thenReturn(Optional.empty());

        GatewayException ex = assertThrows(GatewayException.class,
            () -> userService.createUser("carol", "secret1234567", null, 999L));
        assertEquals(GatewayError.INVALID_REQUEST, ex.getError());
    }

    @Test
    void updateUser_email() {
        User user = activeUser(5L);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User updated = userService.updateUser(5L, "new@test.com", null);

        assertEquals("new@test.com", updated.getEmail());
    }

    @Test
    void updateUser_group() {
        User user = activeUser(5L);
        UserGroup group = group(3L);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(userGroupService.findById(3L)).thenReturn(Optional.of(group));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User updated = userService.updateUser(5L, null, 3L);

        assertEquals(3L, updated.getGroupId());
    }

    @Test
    void updateStatus_disable() {
        User user = activeUser(5L);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User updated = userService.updateStatus(5L, UserStatus.DISABLED);

        assertEquals(UserStatus.DISABLED, updated.getStatus());
    }

    @Test
    void adjustUserQuota_increase() {
        User user = activeUser(5L);
        Quota quota = quota(10L, 1000, 100);
        when(userTenantRelRepository.findFirstByUserIdOrderByJoinedAtAsc(5L))
            .thenReturn(Optional.of(rel(5L, 10L)));
        when(billingService.getQuota(10L)).thenReturn(quota);
        when(billingService.adjustQuota(10L, 5L, 500, "admin quota adjust")).thenReturn(quota);

        userService.adjustUserQuota(5L, 1500);

        verify(billingService).adjustQuota(10L, 5L, 500, "admin quota adjust");
    }

    @Test
    void adjustUserQuota_decrease() {
        Quota quota = quota(10L, 1000, 100);
        when(userTenantRelRepository.findFirstByUserIdOrderByJoinedAtAsc(5L))
            .thenReturn(Optional.of(rel(5L, 10L)));
        when(billingService.getQuota(10L)).thenReturn(quota);
        when(billingService.adjustQuota(10L, 5L, -200, "admin quota adjust")).thenReturn(quota);

        userService.adjustUserQuota(5L, 800);

        verify(billingService).adjustQuota(10L, 5L, -200, "admin quota adjust");
    }

    @Test
    void adjustUserQuota_noChange() {
        Quota quota = quota(10L, 1000, 100);
        when(userTenantRelRepository.findFirstByUserIdOrderByJoinedAtAsc(5L))
            .thenReturn(Optional.of(rel(5L, 10L)));
        when(billingService.getQuota(10L)).thenReturn(quota);

        Quota result = userService.adjustUserQuota(5L, 1000);

        assertEquals(quota, result);
        verify(billingService, never()).adjustQuota(any(), any(), any(Long.class), any());
    }

    private static UserGroup group(long id) {
        UserGroup group = new UserGroup();
        group.setId(id);
        group.setName("g" + id);
        return group;
    }

    private static User activeUser(long id) {
        User user = new User();
        user.setId(id);
        user.setDeleted((short) 0);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    private static Quota quota(long tenantId, long total, long used) {
        Quota quota = new Quota();
        quota.setTenantId(tenantId);
        quota.setType("SUBSCRIPTION");
        quota.setTotalTokens(total);
        quota.setUsedTokens(used);
        return quota;
    }

    private static UserTenantRel rel(long userId, long tenantId) {
        UserTenantRel rel = new UserTenantRel();
        rel.setUserId(userId);
        rel.setTenantId(tenantId);
        return rel;
    }
}
