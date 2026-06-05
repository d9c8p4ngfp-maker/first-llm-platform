package com.first.gateway.repository;

import com.first.gateway.domain.entity.KnowledgeBase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, Long> {

    List<KnowledgeBase> findByTenantIdAndDeletedOrderByUpdatedAtDesc(Long tenantId, Short deleted);

    Optional<KnowledgeBase> findByIdAndTenantIdAndDeleted(Long id, Long tenantId, Short deleted);

    List<KnowledgeBase> findByTenantIdAndVisibilityAndDeletedAndStatus(Long tenantId, String visibility, Short deleted, String status);

    List<KnowledgeBase> findByVisibilityAndDeletedAndStatus(String visibility, Short deleted, String status);
}