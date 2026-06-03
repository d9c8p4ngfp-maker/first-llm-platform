package com.first.gateway.repository;

import com.first.gateway.domain.entity.User;
import com.first.gateway.domain.enums.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    long countByStatus(UserStatus status);
}
