package com.first.gateway.service.user;

import com.first.gateway.domain.entity.User;
import com.first.gateway.domain.entity.UserGroup;
import com.first.gateway.repository.UserGroupRepository;
import com.first.gateway.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class UserGroupService {

    private final UserGroupRepository userGroupRepository;
    private final UserRepository userRepository;

    public UserGroupService(UserGroupRepository userGroupRepository, UserRepository userRepository) {
        this.userGroupRepository = userGroupRepository;
        this.userRepository = userRepository;
    }

    public BigDecimal ratioForUser(Long userId) {
        return userRepository.findById(userId)
            .map(User::getGroupId)
            .flatMap(userGroupRepository::findById)
            .map(UserGroup::getRatio)
            .orElse(BigDecimal.ONE);
    }

    public java.util.Optional<UserGroup> findById(Long id) {
        return userGroupRepository.findById(id);
    }

    public java.util.List<UserGroup> listAll() {
        return userGroupRepository.findAll();
    }

    @org.springframework.transaction.annotation.Transactional
    public UserGroup create(String name, java.math.BigDecimal ratio) {
        UserGroup group = new UserGroup();
        group.setName(name);
        group.setRatio(ratio != null ? ratio : BigDecimal.ONE);
        return userGroupRepository.save(group);
    }

    @org.springframework.transaction.annotation.Transactional
    public UserGroup update(Long id, String name, java.math.BigDecimal ratio) {
        UserGroup group = userGroupRepository.findById(id)
            .orElseThrow(() -> new com.first.gateway.infra.error.GatewayException(
                com.first.gateway.infra.error.GatewayError.INVALID_REQUEST, "group not found"));
        if (name != null) {
            group.setName(name);
        }
        if (ratio != null) {
            group.setRatio(ratio);
        }
        return userGroupRepository.save(group);
    }

    @org.springframework.transaction.annotation.Transactional
    public void delete(Long id) {
        if (userGroupRepository.findByName("default").map(UserGroup::getId).orElse(-1L).equals(id)) {
            throw new com.first.gateway.infra.error.GatewayException(
                com.first.gateway.infra.error.GatewayError.INVALID_REQUEST, "cannot delete default group");
        }
        userGroupRepository.deleteById(id);
    }

    public UserGroup defaultGroup() {
        return userGroupRepository.findByName("default")
            .orElseThrow(() -> new IllegalStateException("default user group missing"));
    }
}
