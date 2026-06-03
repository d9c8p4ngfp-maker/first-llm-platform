package com.first.gateway.service.knowledge;

import com.first.gateway.config.AiServiceProperties;
import com.first.gateway.domain.entity.Channel;
import com.first.gateway.domain.entity.KnowledgeDocument;
import com.first.gateway.infra.ai.AiServiceClient;
import com.first.gateway.infra.crypto.ChannelKeyCrypto;
import com.first.gateway.relay.router.ChannelSelector;
import com.first.gateway.repository.KnowledgeDocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class KnowledgeIndexService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeIndexService.class);

    private final AiServiceClient aiServiceClient;
    private final AiServiceProperties aiServiceProperties;
    private final ChannelSelector channelSelector;
    private final ChannelKeyCrypto channelKeyCrypto;
    private final KnowledgeDocumentRepository documentRepository;

    public KnowledgeIndexService(AiServiceClient aiServiceClient,
                                 AiServiceProperties aiServiceProperties,
                                 ChannelSelector channelSelector,
                                 ChannelKeyCrypto channelKeyCrypto,
                                 KnowledgeDocumentRepository documentRepository) {
        this.aiServiceClient = aiServiceClient;
        this.aiServiceProperties = aiServiceProperties;
        this.channelSelector = channelSelector;
        this.channelKeyCrypto = channelKeyCrypto;
        this.documentRepository = documentRepository;
    }

    @Async
    @Transactional
    public void indexDocumentAsync(Long kbId, KnowledgeDocument doc, Long userId) {
        if (!aiServiceProperties.isRag() || !aiServiceClient.isHealthy()) {
            log.debug("Skip RAG index: AI service unavailable for doc {}", doc.getId());
            return;
        }
        if (doc.getContent() == null || doc.getContent().isBlank()) {
            return;
        }
        try {
            String embedModel = aiServiceProperties.getEmbeddingModel();
            Channel channel = channelSelector.selectForModel(embedModel, userId);

            Map<String, Object> upstream = new LinkedHashMap<>();
            upstream.put("base_url", channel.getBaseUrl());
            upstream.put("api_key", channelKeyCrypto.decrypt(channel.getApiKeyEncrypted()));
            upstream.put("model", embedModel);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("document_id", doc.getId());
            body.put("knowledge_base_id", kbId);
            body.put("content", doc.getContent());
            body.put("file_type", doc.getSourceType() != null ? doc.getSourceType() : "TEXT");
            body.put("embedding_model", embedModel);
            body.put("upstream", upstream);

            Optional<Map<String, Object>> result = aiServiceClient.indexRag(body);
            if (result.isPresent()) {
                doc.setSyncStatus("INDEXED");
                documentRepository.save(doc);
                log.info("Indexed document {} in KB {} via first-ai-service", doc.getId(), kbId);
            } else {
                doc.setSyncStatus("FAILED");
                documentRepository.save(doc);
            }
        } catch (Exception e) {
            log.warn("RAG index failed for doc {}: {}", doc.getId(), e.getMessage());
            doc.setSyncStatus("FAILED");
            documentRepository.save(doc);
        }
    }
}