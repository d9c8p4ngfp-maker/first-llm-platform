package com.first.gateway.service.knowledge;

import com.first.gateway.config.AiServiceProperties;
import com.first.gateway.domain.entity.Channel;
import com.first.gateway.domain.entity.KnowledgeBase;
import com.first.gateway.domain.entity.KnowledgeDocument;
import com.first.gateway.domain.enums.ChannelStatus;
import com.first.gateway.infra.ai.AiServiceClient;
import com.first.gateway.infra.crypto.ChannelKeyCrypto;
import com.first.gateway.infra.error.GatewayError;
import com.first.gateway.infra.error.GatewayException;
import com.first.gateway.repository.ChannelRepository;
import com.first.gateway.repository.KnowledgeBaseRepository;
import com.first.gateway.repository.KnowledgeDocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@Transactional(readOnly = true)
public class KnowledgeBaseService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseService.class);

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final AiServiceClient aiServiceClient;
    private final AiServiceProperties aiServiceProperties;
    private final ChannelRepository channelRepository;
    private final ChannelKeyCrypto channelKeyCrypto;

    public KnowledgeBaseService(KnowledgeBaseRepository knowledgeBaseRepository,
                                KnowledgeDocumentRepository knowledgeDocumentRepository,
                                AiServiceClient aiServiceClient,
                                AiServiceProperties aiServiceProperties,
                                ChannelRepository channelRepository,
                                ChannelKeyCrypto channelKeyCrypto) {
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.knowledgeDocumentRepository = knowledgeDocumentRepository;
        this.aiServiceClient = aiServiceClient;
        this.aiServiceProperties = aiServiceProperties;
        this.channelRepository = channelRepository;
        this.channelKeyCrypto = channelKeyCrypto;
    }

    public List<KnowledgeBase> listByTenant(Long tenantId) {
        return knowledgeBaseRepository.findByTenantIdAndDeletedOrderByUpdatedAtDesc(tenantId, (short) 0);
    }

    public KnowledgeBase requireByTenant(Long id, Long tenantId) {
        return knowledgeBaseRepository.findByIdAndTenantIdAndDeleted(id, tenantId, (short) 0)
            .orElseThrow(() -> new GatewayException(GatewayError.INVALID_REQUEST, "knowledge base not found"));
    }

    public List<KnowledgeDocument> listDocuments(Long kbId, Long tenantId) {
        requireByTenant(kbId, tenantId);
        return knowledgeDocumentRepository.findByKnowledgeBaseIdAndDeletedOrderByUpdatedAtDesc(kbId, (short) 0);
    }

    public KnowledgeDocument requireDocument(Long docId, Long kbId, Long tenantId) {
        KnowledgeDocument doc = knowledgeDocumentRepository.findById(docId)
            .orElseThrow(() -> new GatewayException(GatewayError.INVALID_REQUEST, "document not found"));
        if (!kbId.equals(doc.getKnowledgeBaseId()) || !tenantId.equals(doc.getTenantId()) || doc.getDeleted() != 0) {
            throw new GatewayException(GatewayError.INVALID_REQUEST, "document not found");
        }
        return doc;
    }

    @Transactional
    public KnowledgeBase create(Long tenantId, String name, String description) {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setTenantId(tenantId);
        kb.setName(name != null && !name.isBlank() ? name.trim() : "Untitled");
        kb.setDescription(description);
        kb.setStatus("ACTIVE");
        kb.setVisibility("PRIVATE");
        kb.setDeleted((short) 0);
        return knowledgeBaseRepository.save(kb);
    }

    @Transactional
    public KnowledgeBase update(Long id, Long tenantId, String name, String description) {
        KnowledgeBase kb = requireByTenant(id, tenantId);
        if (name != null && !name.isBlank()) {
            kb.setName(name.trim());
        }
        if (description != null) {
            kb.setDescription(description);
        }
        return knowledgeBaseRepository.save(kb);
    }

    @Transactional
    public void delete(Long id, Long tenantId) {
        KnowledgeBase kb = requireByTenant(id, tenantId);
        kb.setDeleted((short) 1);
        knowledgeBaseRepository.save(kb);
    }

    @Transactional
    public KnowledgeDocument createDocument(Long kbId, Long tenantId, Long userId, String title, String content) {
        requireByTenant(kbId, tenantId);
        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setKnowledgeBaseId(kbId);
        doc.setTenantId(tenantId);
        doc.setTitle(title != null && !title.isBlank() ? title.trim() : "Untitled");
        doc.setContent(content);
        doc.setSourceType("MANUAL");
        doc.setSyncStatus("INDEXING");
        doc.setDeleted((short) 0);
        doc = knowledgeDocumentRepository.save(doc);
        triggerIndex(kbId, doc);
        return doc;
    }

    @Transactional
    public void deleteDocument(Long kbId, Long tenantId, Long docId) {
        KnowledgeDocument doc = requireDocument(docId, kbId, tenantId);
        doc.setDeleted((short) 1);
        knowledgeDocumentRepository.save(doc);
    }

    @Transactional
    public KnowledgeDocument addDocument(Long kbId, Long tenantId, Long userId, String title, String content) {
        return createDocument(kbId, tenantId, userId, title, content);
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> search(Long kbId, Long tenantId, String query, int topK) {
        requireByTenant(kbId, tenantId);

        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        if (aiServiceProperties.isRag() && aiServiceClient.isHealthy()) {
            try {
                Map<String, Object> ragRequest = new LinkedHashMap<>();
                ragRequest.put("query", query);
                ragRequest.put("knowledge_base_ids", List.of(kbId));
                ragRequest.put("top_k", topK);
                ragRequest.put("embedding_model", aiServiceProperties.getEmbeddingModel());
                ragRequest.put("upstream", getDefaultUpstream());

                var chunks = aiServiceClient.queryRag(ragRequest);
                if (chunks.isPresent() && !chunks.get().isEmpty()) {
                    return (List<Map<String, Object>>) (List<?>) chunks.get();
                }
            } catch (Exception e) {
                log.warn("RAG query failed for kb {}: {}", kbId, e.getMessage());
            }
        }

        // Fallback: keyword search
        List<KnowledgeDocument> docs = knowledgeDocumentRepository
            .findByKnowledgeBaseIdAndDeletedOrderByUpdatedAtDesc(kbId, (short) 0);
        if (docs.isEmpty()) {
            return Collections.emptyList();
        }
        String lowerQuery = query.toLowerCase();
        String[] keywords = lowerQuery.split("\\s+");
        return docs.stream()
            .filter(doc -> doc.getContent() != null)
            .map(doc -> {
                String content = doc.getContent().toLowerCase();
                long score = java.util.Arrays.stream(keywords)
                    .filter(kw -> kw.length() >= 2 && content.contains(kw))
                    .count();
                return Map.<String, Object>of(
                    "documentId", doc.getId(),
                    "title", doc.getTitle() != null ? doc.getTitle() : "",
                    "score", score,
                    "snippet", doc.getContent().substring(0, Math.min(200, doc.getContent().length()))
                );
            })
            .filter(m -> (long) m.get("score") > 0)
            .sorted(Comparator.comparingLong(m -> -(long) m.get("score")))
            .limit(topK)
            .toList();
    }

    @Transactional
    public KnowledgeDocument reindexDocument(Long kbId, Long tenantId, Long userId, Long docId) {
        KnowledgeDocument doc = requireDocument(docId, kbId, tenantId);
        doc.setSyncStatus("INDEXING");
        doc = knowledgeDocumentRepository.save(doc);
        triggerIndex(kbId, doc);
        return doc;
    }

    private void triggerIndex(Long kbId, KnowledgeDocument doc) {
        if (!aiServiceProperties.isRag() || !aiServiceClient.isHealthy()) {
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> indexRequest = new LinkedHashMap<>();
                indexRequest.put("document_id", doc.getId());
                indexRequest.put("knowledge_base_id", kbId);
                indexRequest.put("content", doc.getContent());
                indexRequest.put("file_type", doc.getFileType() != null ? doc.getFileType() : "TEXT");
                indexRequest.put("embedding_model", aiServiceProperties.getEmbeddingModel());
                indexRequest.put("upstream", getDefaultUpstream());
                aiServiceClient.indexRag(indexRequest);
                doc.setSyncStatus("INDEXED");
                log.info("RAG index success for doc {}", doc.getId());
            } catch (Exception e) {
                log.error("RAG indexing failed for doc {}: {}", doc.getId(), e.getMessage());
                doc.setSyncStatus("FAILED");
                doc.setIndexError(e.getMessage());
            }
            knowledgeDocumentRepository.save(doc);
        });
    }

    private Map<String, Object> getDefaultUpstream() {
        List<Channel> channels = channelRepository.findByStatusOrderByPriorityDescWeightDesc(ChannelStatus.ACTIVE);
        if (channels.isEmpty()) {
            return Map.of(
                "base_url", "https://api.openai.com",
                "api_key", "",
                "model", aiServiceProperties.getEmbeddingModel()
            );
        }
        Channel channel = channels.getFirst();
        return Map.of(
            "base_url", channel.getBaseUrl(),
            "api_key", channelKeyCrypto.decrypt(channel.getApiKeyEncrypted()),
            "model", aiServiceProperties.getEmbeddingModel()
        );
    }
}
