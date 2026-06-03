package com.first.gateway.repository;

import com.first.gateway.domain.entity.BillingRetry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BillingRetryRepository extends JpaRepository<BillingRetry, Long> {

    List<BillingRetry> findByStatusOrderByUpdatedAtAsc(String status);
}
