package com.first.gateway.repository;
import com.first.gateway.domain.entity.PipelineConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List; import java.util.Optional;
public interface PipelineConfigRepository extends JpaRepository<PipelineConfig, Long> {
    List<PipelineConfig> findByScopeOrUserId(String scope, Long userId);
    Optional<PipelineConfig> findByConfigKeyAndScopeAndUserId(String configKey, String scope, Long userId);
    Optional<PipelineConfig> findByConfigKeyAndUserId(String configKey, Long userId);
}