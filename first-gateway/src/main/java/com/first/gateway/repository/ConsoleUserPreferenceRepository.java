package com.first.gateway.repository;

import com.first.gateway.domain.entity.ConsoleUserPreference;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsoleUserPreferenceRepository extends JpaRepository<ConsoleUserPreference, Long> {
}