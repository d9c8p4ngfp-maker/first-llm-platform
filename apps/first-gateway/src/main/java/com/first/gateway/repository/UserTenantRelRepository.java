package com.first.gateway.repository;

import com.first.gateway.domain.entity.UserTenantRel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserTenantRelRepository extends JpaRepository<UserTenantRel, Long> {

    Optional<UserTenantRel> findFirstByUserIdOrderByJoinedAtAsc(Long userId);

    List<UserTenantRel> findByUserId(Long userId);
}
