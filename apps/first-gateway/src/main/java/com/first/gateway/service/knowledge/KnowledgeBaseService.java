package com.first.gateway.service.knowledge;

import com.first.gateway.config.AiServiceProperties;
import com.first.gateway.domain.entity.AsyncTask;
import com.first.gateway.domain.entity.Channel;
import com.first.gateway.domain.entity.ChannelModel;
import com.first.gateway.domain.entity.KnowledgeBase;
import com.first.gateway.domain.entity.KnowledgeDocument;
import com.first.gateway.domain.enums.SourceType;
import com.first.gateway.domain.enums.SyncStatus;
import com.first.gateway.domain.enums.ModelType;
import com.first.gateway.infra.ai.AiServiceClient;
import com.first.gateway.infra.ai.dto.RagChunkResult;
import com.first.gateway.infra.ai.dto.RagQueryRequest;
import com.first.gateway.infra.ai.dto.RagQueryResponse;
import com.first.gateway.infra.ai.dto.UpstreamConfig;
import com.first.gateway.infra.crypto.ChannelKeyCrypto;
import com.first.gateway.infra.error.GatewayError;
import com.first.gateway.infra.error.GatewayException;
import com.first.gateway.repository.AsyncTaskRepository;
import com.first.gateway.repository.ChannelModelRepository;
import com.first.gateway.repository.ChannelRepository;
import com.first.gateway.repository.KnowledgeBaseRepository;
import com.first.gateway.repository.KnowledgeDocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class KnowledgeBaseService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseService.class);

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final AiServiceClient aiServiceClient;
    private final AiServiceProperties aiServiceProperties;
    private final ChannelRepository channelRepository;
    private final ChannelModelRepository channelModelRepository;
    private final ChannelKeyCrypto channelKeyCrypto;
    private final AsyncTaskRepository asyncTaskRepository;

    public KnowledgeBaseService(KnowledgeBaseRepository knowledgeBaseRepository,
                                KnowledgeDocumentRepository knowledgeDocumentRepository,
                                AiServiceClient aiServiceClient,
                                AiServiceProperties aiServiceProperties,
                                ChannelRepository channelRepository,
                                ChannelModelRepository channelModelRepository,
                                ChannelKeyCrypto channelKeyCrypto,
                                AsyncTaskRepository asyncTaskRepository) {
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.knowledgeDocumentRepository = knowledgeDocumentRepository;
        this.aiServiceClient = aiServiceClient;
        this.aiServiceProperties = aiServiceProperties;
        this.channelRepository = channelRepository;
        this.channelModelRepository = channelModelRepository;
        this.channelKeyCrypto = channelKeyCrypto;
        this.asyncTaskRepository = asyncTaskRepository;
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
    public KnowledgeBase createPublicKnowledgeBase(String name, String description) {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setTenantId(0L);
        kb.setVisibility("PUBLIC");
        kb.setName(name);
        kb.setDescription(description);
        kb.setStatus("ACTIVE");
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
        doc.setSourceType(SourceType.MANUAL);
        doc.setSyncStatus(SyncStatus.PENDING);
        doc.setDeleted((short) 0);
        doc = knowledgeDocumentRepository.save(doc);

        AsyncTask task = new AsyncTask();
        task.setTaskType("DOC_INDEX");
        task.setRefId(doc.getId());
        task.setRefExtra(kbId);
        asyncTaskRepository.save(task);

        return doc;
    }

    @Transactional
    public KnowledgeDocument createDocumentFromFile(Long kbId, Long tenantId, Long userId, String title,
                                                      FileStorageService.FileStorageResult stored) {
        requireByTenant(kbId, tenantId);
        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setKnowledgeBaseId(kbId);
        doc.setTenantId(tenantId);
        doc.setTitle(title != null && !title.isBlank() ? title.trim() : "Untitled");
        doc.setFilePath(stored.filePath());
        doc.setFileType(stored.fileType());
        doc.setFileSize(stored.fileSize());
        doc.setSourceType(SourceType.FILE);
        doc.setSyncStatus(SyncStatus.PENDING);
        doc.setDeleted((short) 0);
        doc = knowledgeDocumentRepository.save(doc);

        AsyncTask task = new AsyncTask();
        task.setTaskType("DOC_INDEX");
        task.setRefId(doc.getId());
        task.setRefExtra(kbId);
        asyncTaskRepository.save(task);

        return doc;
    }

    @Transactional
    public KnowledgeDocument importFromUrl(Long kbId, Long tenantId, Long userId, String url, String title) {
        KnowledgeBase kb = knowledgeBaseRepository.findById(kbId)
            .orElseThrow(() -> new GatewayException(GatewayError.INVALID_REQUEST, "knowledge base not found"));
        if ("PUBLIC".equals(kb.getVisibility())) {
            // Public KB — permission checked by Controller
        } else if (!kb.getTenantId().equals(tenantId)) {
            throw new GatewayException(GatewayError.ACCESS_DENIED);
        }

        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setKnowledgeBaseId(kbId);
        doc.setTenantId(tenantId);
        doc.setTitle(title != null ? title : url);
        doc.setSourceUrl(url);
        doc.setSourceType(SourceType.URL);
        doc.setSyncStatus(SyncStatus.PENDING);
        doc = knowledgeDocumentRepository.save(doc);

        AsyncTask task = new AsyncTask();
        task.setTaskType("DOC_INDEX");
        task.setRefId(doc.getId());
        task.setRefExtra(kbId);
        asyncTaskRepository.save(task);
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

    public List<Map<String, Object>> search(Long kbId, Long tenantId, String query, int topK) {
        requireByTenant(kbId, tenantId);

        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        if (aiServiceProperties.isRag() && aiServiceClient.isHealthy()) {
            try {
                RagQueryRequest ragRequest = new RagQueryRequest(
                    query, List.of(kbId), topK,
                    aiServiceProperties.getEmbeddingModel(),
                    getEmbeddingUpstream());

                var response = aiServiceClient.queryRag(ragRequest);
                if (response.isPresent() && response.get().chunks() != null
                    && !response.get().chunks().isEmpty()) {
                    return response.get().chunks().stream()
                        .map(c -> {
                            Map<String, Object> m = new java.util.LinkedHashMap<>();
                            m.put("document_id", c.documentId());
                            m.put("knowledge_base_id", c.knowledgeBaseId());
                            m.put("content", c.content());
                            m.put("score", c.score());
                            m.put("title", c.metadata() != null ? c.metadata().getOrDefault("title", "") : "");
                            m.put("snippet", c.content() != null && c.content().length() > 200
                                ? c.content().substring(0, 200) : (c.content() != null ? c.content() : ""));
                            return m;
                        })
                        .toList();
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

    private static final double MIN_CONFIDENCE_SCORE = 0.65;

    public Optional<RagQueryResponse> searchAll(Long tenantId, String query, int topK) {
        UpstreamConfig upstream = getEmbeddingUpstream();

        // Stage 1: search user's private KBs
        List<Long> privateKbIds = knowledgeBaseRepository
            .findByTenantIdAndVisibilityAndDeletedAndStatus(tenantId, "PRIVATE", (short) 0, "ACTIVE")
            .stream().map(KnowledgeBase::getId).toList();

        List<RagChunkResult> results = new ArrayList<>();
        if (!privateKbIds.isEmpty()) {
            var privateReq = new RagQueryRequest(query, privateKbIds, topK, upstream.model(), upstream);
            aiServiceClient.queryRag(privateReq)
                .ifPresent(resp -> results.addAll(resp.chunks()));
        }

        // Stage 2: supplement with public KB if private results insufficient
        boolean needPublic = results.size() < topK
            || results.stream().mapToDouble(RagChunkResult::score).max().orElse(0) < MIN_CONFIDENCE_SCORE;

        if (needPublic) {
            List<Long> publicKbIds = knowledgeBaseRepository
                .findByVisibilityAndDeletedAndStatus("PUBLIC", (short) 0, "ACTIVE")
                .stream().map(KnowledgeBase::getId).toList();
            if (!publicKbIds.isEmpty()) {
                int remaining = topK - results.size();
                int publicTopK = Math.max(remaining, topK / 2);
                var publicReq = new RagQueryRequest(query, publicKbIds, publicTopK, upstream.model(), upstream);
                aiServiceClient.queryRag(publicReq)
                    .ifPresent(resp -> results.addAll(resp.chunks()));
            }
        }

        if (results.isEmpty()) return Optional.empty();

        List<RagChunkResult> merged = results.stream()
            .collect(Collectors.toMap(
                c -> c.documentId() + ":" + c.content().hashCode(),
                c -> c, (a, b) -> a.score() >= b.score() ? a : b))
            .values().stream()
            .sorted(Comparator.comparingDouble(RagChunkResult::score).reversed())
            .limit(topK)
            .toList();

        return Optional.of(new RagQueryResponse(merged));
    }

    @Transactional
    public KnowledgeDocument reindexDocument(Long kbId, Long tenantId, Long userId, Long docId) {
        KnowledgeDocument doc = requireDocument(docId, kbId, tenantId);
        doc.setSyncStatus(SyncStatus.PENDING);
        doc = knowledgeDocumentRepository.save(doc);

        AsyncTask task = new AsyncTask();
        task.setTaskType("DOC_INDEX");
        task.setRefId(doc.getId());
        task.setRefExtra(kbId);
        asyncTaskRepository.save(task);

        return doc;
    }

    public UpstreamConfig getEmbeddingUpstream() {
        List<ChannelModel> embeddingModels = channelModelRepository
            .findByModelTypeEnabled(ModelType.EMBEDDING);
        if (embeddingModels.isEmpty()) {
            throw new GatewayException(GatewayError.NO_AVAILABLE_CHANNEL, "No embedding model available");
        }
        ChannelModel cm = embeddingModels.getFirst();
        Channel channel = channelRepository.findById(cm.getChannelId())
            .orElseThrow(() -> new GatewayException(GatewayError.NO_AVAILABLE_CHANNEL));
        return new UpstreamConfig(
            channel.getBaseUrl(),
            channelKeyCrypto.decrypt(channel.getApiKeyEncrypted()),
            cm.getModelName());
    }
}
