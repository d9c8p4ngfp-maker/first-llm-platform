package com.first.gateway.repository;

import com.first.gateway.domain.entity.RedemptionCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RedemptionCodeRepository extends JpaRepository<RedemptionCode, Long> {

    Optional<RedemptionCode> findByCode(String code);
}
