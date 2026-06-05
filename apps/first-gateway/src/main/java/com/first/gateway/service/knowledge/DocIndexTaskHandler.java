package com.first.gateway.service.knowledge;

import com.first.gateway.domain.entity.AsyncTask;
import com.first.gateway.domain.entity.KnowledgeDocument;
import com.first.gateway.domain.enums.SyncStatus;
import com.first.gateway.infra.ai.AiServiceClient;
import com.first.gateway.infra.ai.dto.*;
import com.first.gateway.infra.async.TaskHandler;
import com.first.gateway.repository.KnowledgeDocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component("DOC_INDEX")
public class DocIndexTaskHandler implements TaskHandler {

    private static final Logger log = LoggerFactory.getLogger(DocIndexTaskHandler.class);

    private final KnowledgeDocumentRepository documentRepository;
    private final AiServiceClient aiServiceClient;
    private final KnowledgeBaseService knowledgeBaseService;

    public DocIndexTaskHandler(KnowledgeDocumentRepository documentRepository,
                               AiServiceClient aiServiceClient,
                               KnowledgeBaseService knowledgeBaseService) {
        this.documentRepository = documentRepository;
        this.aiServiceClient = aiServiceClient;
        this.knowledgeBaseService = knowledgeBaseService;
    }

    @Override
    public void execute(AsyncTask task) {
        KnowledgeDocument doc = documentRepository.findById(task.getRefId())
            .orElseThrow(() -> new RuntimeException("文档不存在"));

        UpstreamConfig upstream = knowledgeBaseService.getEmbeddingUpstream();

        if (doc.getSourceUrl() != null && !doc.getSourceUrl().isBlank()) {
            doc.setSyncStatus(SyncStatus.CRAWLING);
            documentRepository.save(doc);
            var crawlResult = aiServiceClient.crawlAndIndex(new CrawlAndIndexRequest(
                doc.getSourceUrl(), task.getRefExtra(), doc.getId(),
                upstream.model(), upstream));
            if (crawlResult.isEmpty()) {
                throw new RuntimeException("crawlAndIndex returned empty result");
            }
        } else {
            doc.setSyncStatus(SyncStatus.INDEXING);
            documentRepository.save(doc);
            var indexResult = aiServiceClient.indexRag(new RagIndexRequest(
                doc.getId(), task.getRefExtra(),
                doc.getContent(), doc.getFilePath(), doc.getFileType(),
                upstream.model(), upstream));
            if (indexResult.isEmpty()) {
                throw new RuntimeException("indexRag returned empty result");
            }
        }
    }

    @Override
    public void onSuccess(AsyncTask task) {
        documentRepository.findById(task.getRefId()).ifPresent(doc -> {
            doc.setSyncStatus(SyncStatus.INDEXED);
            doc.setIndexError(null);
            documentRepository.save(doc);
        });
    }

    @Override
    public void onFailure(AsyncTask task, Exception e) {
        documentRepository.findById(task.getRefId()).ifPresent(doc -> {
            doc.setSyncStatus(task.getRetryCount() >= task.getMaxRetry()
                ? SyncStatus.FAILED : SyncStatus.PENDING);
            doc.setIndexError(e.getMessage());
            documentRepository.save(doc);
        });
    }
}
