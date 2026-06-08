package com.first.gateway.service.user;

import com.first.gateway.domain.entity.Channel;
import com.first.gateway.domain.entity.ChannelModel;
import com.first.gateway.domain.entity.Quota;
import com.first.gateway.domain.entity.Tenant;
import com.first.gateway.domain.entity.User;
import com.first.gateway.domain.entity.UserGroup;
import com.first.gateway.domain.entity.UserTenantRel;
import com.first.gateway.domain.enums.ModelType;
import com.first.gateway.domain.enums.TenantRole;
import com.first.gateway.domain.enums.UserStatus;
import com.first.gateway.infra.error.GatewayError;
import com.first.gateway.infra.error.GatewayException;
import com.first.gateway.repository.ChannelModelRepository;
import com.first.gateway.repository.ChannelRepository;
import com.first.gateway.repository.QuotaRepository;
import com.first.gateway.repository.TenantRepository;
import com.first.gateway.repository.UserRepository;
import com.first.gateway.repository.UserTenantRelRepository;
import com.first.gateway.service.billing.BillingService;
import com.first.gateway.service.channel.ChannelService;
import com.first.gateway.service.system.SystemConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final UserTenantRelRepository userTenantRelRepository;
    private final QuotaRepository quotaRepository;
    private final UserGroupService userGroupService;
    private final SystemConfigService systemConfigService;
    private final BillingService billingService;
    private final PasswordEncoder passwordEncoder;
    private final ChannelModelRepository channelModelRepository;
    private final ChannelRepository channelRepository;
    private final ChannelService channelService;

    public UserService(UserRepository userRepository,
                       TenantRepository tenantRepository,
                       UserTenantRelRepository userTenantRelRepository,
                       QuotaRepository quotaRepository,
                       UserGroupService userGroupService,
                       SystemConfigService systemConfigService,
                       BillingService billingService,
                       PasswordEncoder passwordEncoder,
                       ChannelModelRepository channelModelRepository,
                       ChannelRepository channelRepository,
                       ChannelService channelService) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.userTenantRelRepository = userTenantRelRepository;
        this.quotaRepository = quotaRepository;
        this.userGroupService = userGroupService;
        this.systemConfigService = systemConfigService;
        this.billingService = billingService;
        this.passwordEncoder = passwordEncoder;
        this.channelModelRepository = channelModelRepository;
        this.channelRepository = channelRepository;
        this.channelService = channelService;
    }

    @Transactional
    public User createUser(String username, String password, String email, Long groupId) {
        validatePassword(password);
        if (userRepository.existsByUsername(username.trim())) {
            throw new GatewayException(GatewayError.INVALID_REQUEST, "用户名已被占用");
        }
        if (email != null && !email.isBlank() && userRepository.existsByEmail(email.trim())) {
            throw new GatewayException(GatewayError.INVALID_REQUEST, "邮箱已被注册");
        }
        UserGroup group = resolveGroup(groupId);

        Tenant tenant = new Tenant();
        tenant.setName(username.trim() + " workspace");
        tenant.setType("PERSONAL");
        tenant.setMaxMembers(5);
        tenant = tenantRepository.save(tenant);

        User user = new User();
        user.setUsername(username.trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setGroupId(group.getId());
        user.setStatus(UserStatus.ACTIVE);
        user = userRepository.save(user);

        UserTenantRel rel = new UserTenantRel();
        rel.setUserId(user.getId());
        rel.setTenantId(tenant.getId());
        rel.setRole(TenantRole.MEMBER);
        userTenantRelRepository.save(rel);

        long defaultQuota = systemConfigService.getLong("quota_for_new_user", 100_000L);
        Quota quota = new Quota();
        quota.setTenantId(tenant.getId());
        quota.setType("SUBSCRIPTION");
        quota.setTotalTokens(defaultQuota);
        quotaRepository.save(quota);

        cloneDefaultChannels(tenant.getId(), user.getId());

        return user;
    }

    private void cloneDefaultChannels(Long tenantId, Long userId) {
        Set<Long> clonedChannelIds = new HashSet<>();

        cloneBestChannel(ModelType.CHAT, tenantId, userId, clonedChannelIds);
        cloneBestChannel(ModelType.EMBEDDING, tenantId, userId, clonedChannelIds);

        log.info("cloned {} default channels for new user {}", clonedChannelIds.size(), userId);
    }

    private void cloneBestChannel(ModelType modelType, Long tenantId, Long userId, Set<Long> clonedChannelIds) {
        List<ChannelModel> models = channelModelRepository.findByModelTypeEnabled(modelType);
        if (models.isEmpty()) {
            log.warn("no {} model available for new user {}", modelType, userId);
            return;
        }
        ChannelModel best = models.get(0);
        Long sourceChannelId = best.getChannelId();
        if (!clonedChannelIds.add(sourceChannelId)) {
            return;
        }
        Channel source = channelRepository.findById(sourceChannelId)
            .orElse(null);
        if (source == null) {
            log.warn("channel {} not found for model {}", sourceChannelId, best.getModelName());
            return;
        }
        channelService.cloneForUser(source, tenantId, userId);
        log.info("cloned channel {} ({}) for new user {}", sourceChannelId, best.getModelName(), userId);
    }

    @Transactional
    public User updateUser(Long id, String email, Long groupId) {
        User user = findActiveUser(id);
        if (email != null) {
            user.setEmail(email);
        }
        if (groupId != null) {
            user.setGroupId(resolveGroup(groupId).getId());
        }
        return userRepository.save(user);
    }

    @Transactional
    public User updateStatus(Long id, UserStatus status) {
        User user = findActiveUser(id);
        user.setStatus(status);
        return userRepository.save(user);
    }

    @Transactional
    public Quota adjustUserQuota(Long userId, long totalTokens) {
        Long tenantId = primaryTenantId(userId);
        Quota quota = billingService.getQuota(tenantId);
        long delta = totalTokens - quota.getTotalTokens();
        if (delta == 0) {
            return quota;
        }
        return billingService.adjustQuota(tenantId, userId, delta, "admin quota adjust");
    }

    public Long primaryTenantId(Long userId) {
        return userTenantRelRepository.findFirstByUserIdOrderByJoinedAtAsc(userId)
            .map(UserTenantRel::getTenantId)
            .orElseThrow(() -> new GatewayException(GatewayError.INVALID_REQUEST, "tenant not found"));
    }

    private User findActiveUser(Long id) {
        return userRepository.findById(id)
            .filter(u -> u.getDeleted() == 0)
            .orElseThrow(() -> new GatewayException(GatewayError.INVALID_REQUEST, "user not found"));
    }

    private UserGroup resolveGroup(Long groupId) {
        if (groupId == null) {
            return userGroupService.defaultGroup();
        }
        return userGroupService.findById(groupId)
            .orElseThrow(() -> new GatewayException(GatewayError.INVALID_REQUEST, "group not found"));
    }

    private static final int MIN_PASSWORD_LENGTH = 12;
    private static final String PASSWORD_PATTERN = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$";

    private static void validatePassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new GatewayException(GatewayError.INVALID_REQUEST,
                "Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }
        if (!password.matches(PASSWORD_PATTERN)) {
            throw new GatewayException(GatewayError.INVALID_REQUEST,
                "Password must contain at least one uppercase letter, one lowercase letter and one digit");
        }
    }
}
