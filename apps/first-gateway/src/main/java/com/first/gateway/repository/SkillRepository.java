package com.first.gateway.repository;
import com.first.gateway.domain.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List; import java.util.Optional;
public interface SkillRepository extends JpaRepository<Skill, Long> {
    List<Skill> findByUserIdAndDeletedOrderBySortOrderAsc(Long userId, Short deleted);
    Optional<Skill> findByIdAndUserIdAndDeleted(Long id, Long userId, Short deleted);
}