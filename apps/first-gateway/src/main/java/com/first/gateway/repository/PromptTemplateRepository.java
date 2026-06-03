package com.first.gateway.repository;
import com.first.gateway.domain.entity.PromptTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List; import java.util.Optional;
public interface PromptTemplateRepository extends JpaRepository<PromptTemplate, Long> {
    List<PromptTemplate> findByTenantIdAndDeletedOrderByUpdatedAtDesc(Long tenantId, Short deleted);
    List<PromptTemplate> findByUserIdAndDeletedOrderByUpdatedAtDesc(Long userId, Short deleted);
    Optional<PromptTemplate> findByIdAndTenantIdAndDeleted(Long id, Long tenantId, Short deleted);
}