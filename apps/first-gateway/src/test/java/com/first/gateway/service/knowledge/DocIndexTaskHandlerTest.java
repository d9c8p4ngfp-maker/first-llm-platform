package com.first.gateway.service.knowledge;

import com.first.gateway.domain.entity.AsyncTask;
import com.first.gateway.domain.entity.Channel;
import com.first.gateway.domain.entity.ChannelModel;
import com.first.gateway.domain.entity.KnowledgeDocument;
import com.first.gateway.domain.enums.SyncStatus;
import com.first.gateway.domain.enums.ModelType;
import com.first.gateway.infra.ai.AiServiceClient;
import com.first.gateway.infra.ai.dto.CrawlAndIndexRequest;
import com.first.gateway.infra.ai.dto.CrawlAndIndexResponse;
import com.first.gateway.infra.ai.dto.RagIndexRequest;
import com.first.gateway.infra.ai.dto.RagIndexResponse;
import com.first.gateway.infra.ai.dto.UpstreamConfig;
import com.first.gateway.infra.crypto.ChannelKeyCrypto;
import com.first.gateway.infra.error.GatewayError;
import com.first.gateway.infra.error.GatewayException;
import com.first.gateway.repository.ChannelModelRepository;
import com.first.gateway.repository.ChannelRepository;
import com.first.gateway.repository.KnowledgeDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocIndexTaskHandlerTest {

    @Mock private KnowledgeDocumentRepository documentRepository;
    @Mock private ChannelModelRepository channelModelRepository;
    @Mock private ChannelRepository channelRepository;
    @Mock private ChannelKeyCrypto channelKeyCrypto;
    @Mock private AiServiceClient aiServiceClient;

    @InjectMocks
    private DocIndexTaskHandler handler;

    private AsyncTask task;
    private KnowledgeDocument doc;
    private ChannelModel embeddingModel;
    private Channel channel;

    @BeforeEach
    void setUp() {
        task = new AsyncTask();
        task.setId(1L);
        task.setRefId(100L);
        task.setRefExtra(10L);

        doc = new KnowledgeDocument();
        doc.setId(100L);
        doc.setKnowledgeBaseId(10L);
        doc.setContent("test content");
        doc.setFileType("TEXT");
        doc.setSyncStatus(SyncStatus.PENDING);

        embeddingModel = new ChannelModel();
        embeddingModel.setId(1L);
        embeddingModel.setChannelId(5L);
        embeddingModel.setModelName("text-embedding-3-small");

        channel = new Channel();
        channel.setId(5L);
        channel.setBaseUrl("https://api.openai.com");
        channel.setApiKeyEncrypted("encrypted-key");

        when(channelModelRepository.findByModelTypeEnabled(ModelType.EMBEDDING))
                .thenReturn(List.of(embeddingModel));
        when(channelRepository.findById(5L)).thenReturn(Optional.of(channel));
        when(channelKeyCrypto.decrypt("encrypted-key")).thenReturn("sk-real-key");
    }

    @Test
    void shouldIndexRegularDocument() throws Exception {
        when(documentRepository.findById(100L)).thenReturn(Optional.of(doc));
        when(aiServiceClient.indexRag(any(RagIndexRequest.class)))
                .thenReturn(Optional.of(new RagIndexResponse(100L, 3, 150, "ok")));

        handler.execute(task);

        assertThat(doc.getSyncStatus()).isEqualTo(SyncStatus.PENDING);
        assertThat(doc.getChunkCount()).isEqualTo(3);
        assertThat(doc.getWordCount()).isEqualTo(150);
        verify(aiServiceClient).indexRag(any(RagIndexRequest.class));
        verify(aiServiceClient, never()).crawlAndIndex(any());
    }

    @Test
    void shouldCrawlUrlDocument() throws Exception {
        doc.setSourceUrl("https://example.com/doc");
        doc.setContent(null);

        when(documentRepository.findById(100L)).thenReturn(Optional.of(doc));
        when(aiServiceClient.crawlAndIndex(any(CrawlAndIndexRequest.class)))
                .thenReturn(Optional.of(new CrawlAndIndexResponse("ok", "Title", 500, 2)));

        handler.execute(task);

        assertThat(doc.getSyncStatus()).isEqualTo(SyncStatus.PENDING);
        assertThat(doc.getChunkCount()).isEqualTo(2);
        assertThat(doc.getWordCount()).isEqualTo(500);
        verify(aiServiceClient).crawlAndIndex(any(CrawlAndIndexRequest.class));
        verify(aiServiceClient, never()).indexRag(any());
    }

    @Test
    void execute_shouldThrowIfDocumentNotFound() {
        when(documentRepository.findById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> handler.execute(task))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("文档不存在");

        verifyNoInteractions(aiServiceClient);
    }

    @Test
    void execute_shouldThrowWhenNoEmbeddingModel() {
        when(channelModelRepository.findByModelTypeEnabled(ModelType.EMBEDDING))
                .thenReturn(List.of());

        when(documentRepository.findById(100L)).thenReturn(Optional.of(doc));

        assertThatThrownBy(() -> handler.execute(task))
                .isInstanceOf(GatewayException.class)
                .satisfies(ex -> {
                    GatewayException ge = (GatewayException) ex;
                    assertThat(ge.getError()).isEqualTo(GatewayError.NO_AVAILABLE_CHANNEL);
                });
    }

    @Test
    void onSuccess_shouldSetIndexed() {
        when(documentRepository.findById(100L)).thenReturn(Optional.of(doc));

        handler.onSuccess(task);

        assertThat(doc.getSyncStatus()).isEqualTo(SyncStatus.INDEXED);
        assertThat(doc.getIndexError()).isNull();
        verify(documentRepository).save(doc);
    }

    @Test
    void onFailure_retriesExhausted_shouldSetFailed() {
        task.setRetryCount(3);
        task.setMaxRetry(3);
        when(documentRepository.findById(100L)).thenReturn(Optional.of(doc));

        handler.onFailure(task, new RuntimeException("index failed"));

        assertThat(doc.getSyncStatus()).isEqualTo(SyncStatus.FAILED);
        assertThat(doc.getIndexError()).isEqualTo("index failed");
        verify(documentRepository).save(doc);
    }

    @Test
    void onFailure_retriesLeft_shouldSetPending() {
        task.setRetryCount(1);
        task.setMaxRetry(3);
        when(documentRepository.findById(100L)).thenReturn(Optional.of(doc));

        handler.onFailure(task, new RuntimeException("temporary error"));

        assertThat(doc.getSyncStatus()).isEqualTo(SyncStatus.PENDING);
        assertThat(doc.getIndexError()).isEqualTo("temporary error");
        verify(documentRepository).save(doc);
    }

    @Test
    void onFailure_exactlyAtMaxRetry_shouldSetFailed() {
        // retryCount >= maxRetry → FAILED (3 >= 3 is true)
        task.setRetryCount(3);
        task.setMaxRetry(3);
        when(documentRepository.findById(100L)).thenReturn(Optional.of(doc));

        handler.onFailure(task, new RuntimeException("exhausted"));

        assertThat(doc.getSyncStatus()).isEqualTo(SyncStatus.FAILED);
    }

    @Test
    void onSuccess_shouldBeNoopIfDocumentNotFound() {
        when(documentRepository.findById(100L)).thenReturn(Optional.empty());

        handler.onSuccess(task);

        verify(documentRepository, never()).save(any());
    }

    @Test
    void indexRagRequest_shouldIncludeAllFields() throws Exception {
        when(documentRepository.findById(100L)).thenReturn(Optional.of(doc));
        when(aiServiceClient.indexRag(any(RagIndexRequest.class)))
                .thenReturn(Optional.of(new RagIndexResponse(100L, 5, 200, "ok")));

        handler.execute(task);

        ArgumentCaptor<RagIndexRequest> captor = ArgumentCaptor.forClass(RagIndexRequest.class);
        verify(aiServiceClient).indexRag(captor.capture());

        RagIndexRequest req = captor.getValue();
        assertThat(req.documentId()).isEqualTo(100L);
        assertThat(req.knowledgeBaseId()).isEqualTo(10L);
        assertThat(req.content()).isEqualTo("test content");
        assertThat(req.fileType()).isEqualTo("TEXT");
        assertThat(req.embeddingModel()).isEqualTo("text-embedding-3-small");
        assertThat(req.upstream()).isNotNull();
        assertThat(req.upstream().baseUrl()).isEqualTo("https://api.openai.com");
        assertThat(req.upstream().apiKey()).isEqualTo("sk-real-key");
    }

    @Test
    void crawlAndIndexRequest_shouldIncludeUrl() throws Exception {
        doc.setSourceUrl("https://example.com/doc");
        doc.setContent(null);

        when(documentRepository.findById(100L)).thenReturn(Optional.of(doc));
        when(aiServiceClient.crawlAndIndex(any(CrawlAndIndexRequest.class)))
                .thenReturn(Optional.of(new CrawlAndIndexResponse("ok", "Title", 500, 2)));

        handler.execute(task);

        ArgumentCaptor<CrawlAndIndexRequest> captor = ArgumentCaptor.forClass(CrawlAndIndexRequest.class);
        verify(aiServiceClient).crawlAndIndex(captor.capture());

        CrawlAndIndexRequest req = captor.getValue();
        assertThat(req.url()).isEqualTo("https://example.com/doc");
        assertThat(req.knowledgeBaseId()).isEqualTo(10L);
        assertThat(req.documentId()).isEqualTo(100L);
        assertThat(req.upstream()).isNotNull();
    }

    @Test
    void shouldHandleFileDocument() throws Exception {
        doc.setContent(null);
        doc.setFilePath("/uploads/test.pdf");
        doc.setFileType("PDF");

        when(documentRepository.findById(100L)).thenReturn(Optional.of(doc));
        when(aiServiceClient.indexRag(any(RagIndexRequest.class)))
                .thenReturn(Optional.of(new RagIndexResponse(100L, 3, 150, "ok")));

        handler.execute(task);

        ArgumentCaptor<RagIndexRequest> captor = ArgumentCaptor.forClass(RagIndexRequest.class);
        verify(aiServiceClient).indexRag(captor.capture());

        RagIndexRequest req = captor.getValue();
        assertThat(req.filePath()).isEqualTo("/uploads/test.pdf");
        assertThat(req.fileType()).isEqualTo("PDF");
        assertThat(doc.getChunkCount()).isEqualTo(3);
        assertThat(doc.getWordCount()).isEqualTo(150);
    }

    @Test
    void crawl_shouldUpdateTitleWhenDefaultUrlTitle() throws Exception {
        doc.setSourceUrl("https://example.com/doc");
        doc.setTitle("https://example.com/doc");
        doc.setContent(null);

        when(documentRepository.findById(100L)).thenReturn(Optional.of(doc));
        when(aiServiceClient.crawlAndIndex(any(CrawlAndIndexRequest.class)))
                .thenReturn(Optional.of(new CrawlAndIndexResponse("ok", "Actual Page Title", 500, 2)));

        handler.execute(task);

        assertThat(doc.getTitle()).isEqualTo("Actual Page Title");
        assertThat(doc.getChunkCount()).isEqualTo(2);
        assertThat(doc.getWordCount()).isEqualTo(500);
    }
}
