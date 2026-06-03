package com.first.gateway.repository;
import com.first.gateway.domain.entity.PromptFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List; import java.util.Optional;
public interface PromptFavoriteRepository extends JpaRepository<PromptFavorite, Long> {
    List<PromptFavorite> findByUserId(Long userId);
    Optional<PromptFavorite> findByUserIdAndPromptTemplateId(Long userId, Long promptTemplateId);
}