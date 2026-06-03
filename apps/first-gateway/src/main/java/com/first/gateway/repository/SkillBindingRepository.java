package com.first.gateway.repository;
import com.first.gateway.domain.entity.SkillBinding;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface SkillBindingRepository extends JpaRepository<SkillBinding, Long> {
    List<SkillBinding> findBySkillId(Long skillId);
}