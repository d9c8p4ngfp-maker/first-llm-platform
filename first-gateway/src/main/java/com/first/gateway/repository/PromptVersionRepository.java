package com.first.gateway.repository;
import com.first.gateway.domain.entity.PromptVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List; import java.util.Optional;
public interface PromptVersionRepository extends JpaRepository<PromptVersion, Long> {
    List<PromptVersion> findByTemplateIdOrderByCreatedAtDesc(Long templateId);
    Optional<PromptVersion> findFirstByTemplateIdOrderByCreatedAtDesc(Long templateId);
}