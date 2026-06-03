package com.first.gateway.repository;

import com.first.gateway.domain.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    List<Conversation> findByTenantIdAndStatusOrderByLastMessageAtDesc(Long tenantId, String status);

    Optional<Conversation> findByIdAndTenantId(Long id, Long tenantId);
}
