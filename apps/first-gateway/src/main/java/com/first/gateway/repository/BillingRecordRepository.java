package com.first.gateway.repository;

import com.first.gateway.domain.entity.BillingRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BillingRecordRepository extends JpaRepository<BillingRecord, Long> {

    List<BillingRecord> findByTenantIdOrderByCreatedAtDesc(Long tenantId);
}
