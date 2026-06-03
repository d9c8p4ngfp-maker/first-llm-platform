package com.first.gateway.service.knowledge;

import com.first.gateway.domain.entity.KnowledgeBase;
import com.first.gateway.domain.entity.KnowledgeDocument;
import com.first.gateway.infra.error.GatewayError;
import com.first.gateway.infra.error.GatewayException;
import com.first.gateway.repository.KnowledgeBaseRepository;
import com.first.gateway.repository.KnowledgeDocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class KnowledgeBaseService {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;

    public KnowledgeBaseService(KnowledgeBaseRepository knowledgeBaseRepository,
                                KnowledgeDocumentRepository knowledgeDocumentRepository) {
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.knowledgeDocumentRepository = knowledgeDocumentRepository;
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
        doc.setSyncStatus("STORED");
        doc.setDeleted((short) 0);
        return knowledgeDocumentRepository.save(doc);
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

    public List<Map<String, Object>> search(Long kbId, Long tenantId, String query, int topK) {
        requireByTenant(kbId, tenantId);
        List<KnowledgeDocument> docs = knowledgeDocumentRepository
            .findByKnowledgeBaseIdAndDeletedOrderByUpdatedAtDesc(kbId, (short) 0);

        String lowerQuery = query.toLowerCase();
        String[] keywords = lowerQuery.split("\\s+");

        return docs.stream()
            .filter(doc -> doc.getContent() != null)
            .map(doc -> {
                String content = doc.getContent().toLowerCase();
                long score = Arrays.stream(keywords)
                    .filter(kw -> kw.length() >= 2 && content.contains(kw))
                    .count();
                return Map.entry(doc, score);
            })
            .filter(e -> e.getValue() > 0)
            .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
            .limit(topK)
            .map(e -> {
                Map<String, Object> result = new LinkedHashMap<>();
                KnowledgeDocument doc = e.getKey();
                result.put("document_id", doc.getId());
                result.put("title", doc.getTitle());
                result.put("content", doc.getContent().length() > 500
                    ? doc.getContent().substring(0, 500) + "..." : doc.getContent());
                result.put("score", e.getValue());
                return result;
            })
            .toList();
    }

    @Transactional
    public KnowledgeDocument reindexDocument(Long kbId, Long tenantId, Long userId, Long docId) {
        KnowledgeDocument doc = requireDocument(docId, kbId, tenantId);
        doc.setSyncStatus("STORED");
        doc.setIndexError(null);
        return knowledgeDocumentRepository.save(doc);
    }
}
