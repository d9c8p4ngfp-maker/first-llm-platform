package com.first.gateway.service.skill;
import com.first.gateway.domain.entity.Skill; import com.first.gateway.domain.entity.SkillBinding;
import com.first.gateway.infra.error.GatewayError; import com.first.gateway.infra.error.GatewayException;
import com.first.gateway.repository.SkillBindingRepository; import com.first.gateway.repository.SkillRepository;
import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service @Transactional(readOnly = true)
public class SkillService {
    private final SkillRepository skillRepository; private final SkillBindingRepository bindingRepository;
    public SkillService(SkillRepository skillRepository, SkillBindingRepository bindingRepository) {
        this.skillRepository = skillRepository; this.bindingRepository = bindingRepository;
    }
    public List<Skill> list(Long userId) { return skillRepository.findByUserIdAndDeletedOrderBySortOrderAsc(userId, (short) 0); }
    public Skill require(Long id, Long userId) {
        return skillRepository.findByIdAndUserIdAndDeleted(id, userId, (short) 0)
            .orElseThrow(() -> new GatewayException(GatewayError.INVALID_REQUEST, "skill not found"));
    }
    public List<SkillBinding> bindings(Long skillId) { return bindingRepository.findBySkillId(skillId); }
    @Transactional public Skill create(Long tenantId, Long userId, String name, String description, String suggestedModel) {
        Skill s = new Skill(); s.setTenantId(tenantId); s.setUserId(userId); s.setName(name); s.setDescription(description);
        s.setSuggestedModel(suggestedModel); s.setDeleted((short) 0); return skillRepository.save(s);
    }
    @Transactional public Skill update(Long id, Long userId, String name, String description, String suggestedModel) {
        Skill s = require(id, userId);
        if (name != null) s.setName(name); if (description != null) s.setDescription(description);
        if (suggestedModel != null) s.setSuggestedModel(suggestedModel); return skillRepository.save(s);
    }
    @Transactional public void delete(Long id, Long userId) { Skill s = require(id, userId); s.setDeleted((short) 1); skillRepository.save(s); }
    @Transactional public Skill toggle(Long id, Long userId) { Skill s = require(id, userId); s.setEnabled(s.getEnabled() == 1 ? (short) 0 : (short) 1); return skillRepository.save(s); }
    @Transactional public SkillBinding addBinding(Long skillId, Long userId, String type, Long bindingId) {
        require(skillId, userId); SkillBinding b = new SkillBinding(); b.setSkillId(skillId); b.setBindingType(type); b.setBindingId(bindingId);
        return bindingRepository.save(b);
    }
    @Transactional public void removeBinding(Long skillId, Long userId, Long bindingId) {
        require(skillId, userId); bindingRepository.deleteById(bindingId);
    }
}