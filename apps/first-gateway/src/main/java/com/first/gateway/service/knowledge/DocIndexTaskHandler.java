package com.first.gateway.service.knowledge;

import com.first.gateway.domain.entity.AsyncTask;
import com.first.gateway.domain.entity.Channel;
import com.first.gateway.domain.entity.ChannelModel;
import com.first.gateway.domain.entity.KnowledgeDocument;
import com.first.gateway.domain.enums.SyncStatus;
import com.first.gateway.infra.ai.AiServiceClient;
import com.first.gateway.infra.ai.dto.*;
import com.first.gateway.infra.async.TaskHandler;
import com.first.gateway.infra.crypto.ChannelKeyCrypto;
import com.first.gateway.infra.error.GatewayError;
import com.first.gateway.infra.error.GatewayException;
import com.first.gateway.repository.ChannelModelRepository;
import com.first.gateway.repository.ChannelRepository;
import com.first.gateway.repository.KnowledgeDocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("DOC_INDEX")
public class DocIndexTaskHandler implements TaskHandler {

    private static final Logger log = LoggerFactory.getLogger(DocIndexTaskHandler.class);

    private final KnowledgeDocumentRepository documentRepository;
    private final ChannelModelRepository channelModelRepository;
    private final ChannelRepository channelRepository;
    private final ChannelKeyCrypto channelKeyCrypto;
    private final AiServiceClient aiServiceClient;

    public DocIndexTaskHandler(KnowledgeDocumentRepository documentRepository,
                               ChannelModelRepository channelModelRepository,
                               ChannelRepository channelRepository,
                               ChannelKeyCrypto channelKeyCrypto,
                               AiServiceClient aiServiceClient) {
        this.documentRepository = documentRepository;
        this.channelModelRepository = channelModelRepository;
        this.channelRepository = channelRepository;
        this.channelKeyCrypto = channelKeyCrypto;
        this.aiServiceClient = aiServiceClient;
    }

    @Override
    public void execute(AsyncTask task) {
        KnowledgeDocument doc = documentRepository.findById(task.getRefId())
            .orElseThrow(() -> new RuntimeException("文档不存在"));

        UpstreamConfig upstream = getEmbeddingUpstream();

        if (doc.getSourceUrl() != null && !doc.getSourceUrl().isBlank()) {
            doc.setSyncStatus(SyncStatus.CRAWLING);
            documentRepository.save(doc);
            aiServiceClient.crawlAndIndex(new CrawlAndIndexRequest(
                doc.getSourceUrl(), task.getRefExtra(), doc.getId(),
                upstream.model(), upstream));
        } else {
            doc.setSyncStatus(SyncStatus.INDEXING);
            documentRepository.save(doc);
            aiServiceClient.indexRag(new RagIndexRequest(
                doc.getId(), task.getRefExtra(),
                doc.getContent(), doc.getFilePath(), doc.getFileType(),
                upstream.model(), upstream));
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

    private UpstreamConfig getEmbeddingUpstream() {
        List<ChannelModel> models = channelModelRepository.findByModelTypeEnabled("EMBEDDING");
        if (models.isEmpty()) {
            throw new GatewayException(GatewayError.NO_AVAILABLE_CHANNEL, "No embedding model available");
        }
        ChannelModel cm = models.getFirst();
        Channel channel = channelRepository.findById(cm.getChannelId())
            .orElseThrow(() -> new GatewayException(GatewayError.NO_AVAILABLE_CHANNEL));
        return new UpstreamConfig(channel.getBaseUrl(),
            channelKeyCrypto.decrypt(channel.getApiKeyEncrypted()), cm.getModelName());
    }
}
