package com.first.gateway.service.conversation;

import com.first.gateway.domain.entity.Conversation;
import com.first.gateway.domain.entity.ConversationMessage;
import com.first.gateway.infra.error.GatewayError;
import com.first.gateway.infra.error.GatewayException;
import com.first.gateway.repository.ConversationMessageRepository;
import com.first.gateway.repository.ConversationRepository;
import com.first.gateway.service.profile.MemoryExtractionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ConversationService {

    private static final Set<String> ALLOWED_MESSAGE_ROLES = Set.of("user", "assistant");

    private final ConversationRepository conversationRepository;
    private final ConversationMessageRepository messageRepository;
    private final MemoryExtractionService memoryExtractionService;

    public ConversationService(ConversationRepository conversationRepository,
                               ConversationMessageRepository messageRepository,
                               MemoryExtractionService memoryExtractionService) {
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.memoryExtractionService = memoryExtractionService;
    }

    public List<Conversation> listByTenant(Long tenantId) {
        return conversationRepository.findByTenantIdAndStatusOrderByLastMessageAtDesc(tenantId, "ACTIVE");
    }

    @Transactional
    public Conversation create(Long tenantId) {
        Conversation conversation = new Conversation();
        conversation.setTenantId(tenantId);
        conversation.setSessionId(UUID.randomUUID().toString().replace("-", ""));
        conversation.setSummary("\u65b0\u5bf9\u8bdd");
        conversation.setStatus("ACTIVE");
        conversation.setLastMessageAt(Instant.now());
        return conversationRepository.save(conversation);
    }

    public Conversation requireByTenant(Long id, Long tenantId) {
        return conversationRepository.findByIdAndTenantId(id, tenantId)
            .orElseThrow(() -> new GatewayException(GatewayError.INVALID_REQUEST, "conversation not found"));
    }

    public List<ConversationMessage> listMessages(Long conversationId, Long tenantId) {
        requireByTenant(conversationId, tenantId);
        return messageRepository.findByConversationIdOrderByCreatedAtAscIdAsc(conversationId);
    }

    @Transactional
    public Conversation rename(Long id, Long tenantId, String title) {
        Conversation conversation = requireByTenant(id, tenantId);
        if (title != null && !title.isBlank()) {
            conversation.setSummary(title.trim());
        }
        return conversationRepository.save(conversation);
    }

    @Transactional
    public void delete(Long id, Long tenantId) {
        Conversation conversation = requireByTenant(id, tenantId);
        messageRepository.deleteByConversationId(conversation.getId());
        conversationRepository.delete(conversation);
    }

    @Transactional
    public ConversationMessage appendMessage(Long conversationId, Long tenantId, Long userId,
                                             String role, String content) {
        if (role == null || !ALLOWED_MESSAGE_ROLES.contains(role.toLowerCase())) {
            throw new GatewayException(GatewayError.INVALID_REQUEST, "Invalid message role");
        }
        Conversation conversation = requireByTenant(conversationId, tenantId);
        ConversationMessage message = new ConversationMessage();
        message.setConversationId(conversation.getId());
        message.setRole(role);
        message.setContent(content);
        messageRepository.save(message);
        conversation.setMessageCount(conversation.getMessageCount() + 1);
        conversation.setLastMessageAt(Instant.now());
        if (conversation.getMessageCount() <= 1 && content != null && !content.isBlank()) {
            String title = content.length() > 30 ? content.substring(0, 30) + "..." : content;
            conversation.setSummary(title);
        }
        conversationRepository.save(conversation);
        if ("assistant".equalsIgnoreCase(role) && content != null && !content.isBlank()) {
            findLatestUserMessage(conversation.getId()).ifPresent(userMessage ->
                memoryExtractionService.extractAfterChat(
                    userId, tenantId, conversation.getId(), userMessage, content));
        }
        return message;
    }

    private java.util.Optional<String> findLatestUserMessage(Long conversationId) {
        List<ConversationMessage> messages =
            messageRepository.findByConversationIdOrderByCreatedAtAscIdAsc(conversationId);
        for (int i = messages.size() - 1; i >= 0; i--) {
            ConversationMessage msg = messages.get(i);
            if ("user".equalsIgnoreCase(msg.getRole())
                && msg.getContent() != null
                && !msg.getContent().isBlank()) {
                return java.util.Optional.of(msg.getContent());
            }
        }
        return java.util.Optional.empty();
    }
}
