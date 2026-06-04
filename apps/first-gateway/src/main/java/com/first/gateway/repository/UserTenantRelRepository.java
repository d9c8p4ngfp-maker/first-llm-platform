package com.first.gateway.repository;

import com.first.gateway.domain.entity.UserTenantRel;
import com.first.gateway.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserTenantRelRepository extends JpaRepository<UserTenantRel, Long> {

    Optional<UserTenantRel> findFirstByUserIdOrderByJoinedAtAsc(Long userId);

    List<UserTenantRel> findByUserId(Long userId);

    Optional<UserTenantRel> findByUserIdAndTenantId(Long userId, Long tenantId);

    @Query("SELECT r.user FROM UserTenantRel r WHERE r.tenantId = :tenantId")
    List<User> findUsersByTenantId(@Param("tenantId") Long tenantId);
}
