package com.first.gateway.service.knowledge;

import com.first.gateway.config.AiServiceProperties;
import com.first.gateway.domain.entity.AsyncTask;
import com.first.gateway.domain.entity.Channel;
import com.first.gateway.domain.entity.ChannelModel;
import com.first.gateway.domain.entity.KnowledgeBase;
import com.first.gateway.domain.entity.KnowledgeDocument;
import com.first.gateway.domain.enums.SourceType;
import com.first.gateway.domain.enums.ModelType;
import com.first.gateway.domain.enums.SyncStatus;
import com.first.gateway.infra.ai.AiServiceClient;
import com.first.gateway.infra.ai.dto.RagChunkResult;
import com.first.gateway.infra.ai.dto.RagQueryResponse;
import com.first.gateway.infra.crypto.ChannelKeyCrypto;
import com.first.gateway.infra.error.GatewayError;
import com.first.gateway.infra.error.GatewayException;
import com.first.gateway.repository.AsyncTaskRepository;
import com.first.gateway.repository.ChannelModelRepository;
import com.first.gateway.repository.ChannelRepository;
import com.first.gateway.repository.KnowledgeBaseRepository;
import com.first.gateway.repository.KnowledgeDocumentRepository;
import com.first.gateway.service.system.SystemConfigService;
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
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class KnowledgeBaseServiceIntegrationTest {

    @Mock private KnowledgeBaseRepository knowledgeBaseRepository;
    @Mock private KnowledgeDocumentRepository knowledgeDocumentRepository;
    @Mock private AiServiceClient aiServiceClient;
    @Mock private AiServiceProperties aiServiceProperties;
    @Mock private ChannelRepository channelRepository;
    @Mock private ChannelModelRepository channelModelRepository;
    @Mock private ChannelKeyCrypto channelKeyCrypto;
    @Mock private AsyncTaskRepository asyncTaskRepository;
    @Mock private SystemConfigService systemConfigService;

    @InjectMocks
    private KnowledgeBaseService knowledgeBaseService;

    @BeforeEach
    void setUp() {
        when(aiServiceProperties.isRag()).thenReturn(true);
        when(systemConfigService.getString(any(), any()))
                .thenAnswer(inv -> inv.getArgument(1));
    }

    @Test
    void searchAll_highConfidencePrivateResults_shouldNotQueryPublic() {
        KnowledgeBase privateKb = new KnowledgeBase();
        privateKb.setId(1L);
        privateKb.setVisibility("PRIVATE");
        when(knowledgeBaseRepository
                .findByTenantIdAndVisibilityAndDeletedAndStatus(10L, "PRIVATE", (short) 0, "ACTIVE"))
                .thenReturn(List.of(privateKb));

        // Set up embedding upstream
        ChannelModel embeddingModel = new ChannelModel();
        embeddingModel.setId(1L);
        embeddingModel.setChannelId(5L);
        embeddingModel.setModelName("text-embedding-3-small");
        when(channelModelRepository.findByModelTypeEnabled(ModelType.EMBEDDING))
                .thenReturn(List.of(embeddingModel));
        Channel channel = new Channel();
        channel.setId(5L);
        channel.setBaseUrl("https://api.example.com");
        channel.setApiKeyEncrypted("enc-key");
        when(channelRepository.findById(5L)).thenReturn(Optional.of(channel));
        when(channelKeyCrypto.decrypt("enc-key")).thenReturn("sk-test");

        // High-confidence private results → no need to supplement with public
        when(aiServiceClient.isRagEnabled()).thenReturn(true);
        when(aiServiceClient.queryRag(any()))
                .thenReturn(Optional.of(new RagQueryResponse(List.of(
                        new RagChunkResult(1L, 1L, "highly relevant content", 0.88,
                                Map.of("title", "Private KB")),
                        new RagChunkResult(2L, 1L, "another relevant match", 0.82,
                                Map.of("title", "Private KB 2")),
                        new RagChunkResult(3L, 1L, "third match", 0.75,
                                Map.of("title", "Private KB 3")),
                        new RagChunkResult(4L, 1L, "fourth match", 0.70,
                                Map.of("title", "Private KB 4")),
                        new RagChunkResult(5L, 1L, "fifth match", 0.68,
                                Map.of("title", "Private KB 5"))
                ))));

        var result = knowledgeBaseService.searchAll(10L, "test query", 5);

        assertThat(result).isPresent();
        // High-confidence private results → public KBs should NOT be queried
        verify(knowledgeBaseRepository, never())
                .findByVisibilityAndDeletedAndStatus(any(), any(), any());
    }

    @Test
    void searchAll_privateResultsInsufficient_shouldSupplementWithPublic() {
        // Set up embedding upstream
        ChannelModel embeddingModel = new ChannelModel();
        embeddingModel.setId(1L);
        embeddingModel.setChannelId(5L);
        embeddingModel.setModelName("text-embedding-3-small");
        when(channelModelRepository.findByModelTypeEnabled(ModelType.EMBEDDING))
                .thenReturn(List.of(embeddingModel));
        Channel channel = new Channel();
        channel.setId(5L);
        channel.setBaseUrl("https://api.example.com");
        channel.setApiKeyEncrypted("enc-key");
        when(channelRepository.findById(5L)).thenReturn(Optional.of(channel));
        when(channelKeyCrypto.decrypt("enc-key")).thenReturn("sk-test");

        // Private KB
        KnowledgeBase privateKb = new KnowledgeBase();
        privateKb.setId(1L);
        privateKb.setVisibility("PRIVATE");
        when(knowledgeBaseRepository
                .findByTenantIdAndVisibilityAndDeletedAndStatus(10L, "PRIVATE", (short) 0, "ACTIVE"))
                .thenReturn(List.of(privateKb));

        // Public KB
        KnowledgeBase publicKb = new KnowledgeBase();
        publicKb.setId(2L);
        publicKb.setVisibility("PUBLIC");
        when(knowledgeBaseRepository
                .findByVisibilityAndDeletedAndStatus("PUBLIC", (short) 0, "ACTIVE"))
                .thenReturn(List.of(publicKb));

        // Private RAG returns 0 results → triggers public fallback
        when(aiServiceClient.isRagEnabled()).thenReturn(true);
        when(aiServiceClient.queryRag(any()))
                .thenReturn(Optional.of(new RagQueryResponse(List.of())))
                .thenReturn(Optional.of(new RagQueryResponse(List.of())));

        var result = knowledgeBaseService.searchAll(10L, "test query", 5);

        assertThat(result).isEmpty();
        verify(knowledgeBaseRepository)
                .findByVisibilityAndDeletedAndStatus("PUBLIC", (short) 0, "ACTIVE");
    }

    @Test
    void importFromUrl_publicKb_shouldAllowAnyTenant() {
        KnowledgeBase publicKb = new KnowledgeBase();
        publicKb.setId(1L);
        publicKb.setVisibility("PUBLIC");
        publicKb.setTenantId(0L);
        when(knowledgeBaseRepository.findById(1L)).thenReturn(Optional.of(publicKb));
        when(knowledgeDocumentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(asyncTaskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var doc = knowledgeBaseService.importFromUrl(1L, 999L, 1L, "https://example.com", "Test");

        assertThat(doc.getSourceUrl()).isEqualTo("https://example.com");
        assertThat(doc.getSyncStatus()).isEqualTo(SyncStatus.PENDING);
        assertThat(doc.getSourceType()).isEqualTo(SourceType.URL);
        assertThat(doc.getTenantId()).isEqualTo(999L);

        ArgumentCaptor<AsyncTask> taskCaptor = ArgumentCaptor.forClass(AsyncTask.class);
        verify(asyncTaskRepository).save(taskCaptor.capture());
        AsyncTask task = taskCaptor.getValue();
        assertThat(task.getTaskType()).isEqualTo("DOC_INDEX");
    }

    @Test
    void importFromUrl_privateKb_wrongTenant_shouldThrow() {
        KnowledgeBase privateKb = new KnowledgeBase();
        privateKb.setId(1L);
        privateKb.setVisibility("PRIVATE");
        privateKb.setTenantId(100L);
        when(knowledgeBaseRepository.findById(1L)).thenReturn(Optional.of(privateKb));

        assertThatThrownBy(() ->
                knowledgeBaseService.importFromUrl(1L, 999L, 1L, "https://example.com", "Test"))
                .isInstanceOf(GatewayException.class)
                .satisfies(ex -> {
                    GatewayException ge = (GatewayException) ex;
                    assertThat(ge.getError()).isEqualTo(GatewayError.ACCESS_DENIED);
                });
    }

    @Test
    void createPublicKnowledgeBase_shouldSetTenantIdZero() {
        when(knowledgeBaseRepository.save(any())).thenAnswer(inv -> {
            KnowledgeBase kb = inv.getArgument(0);
            kb.setId(1L);
            return kb;
        });

        var kb = knowledgeBaseService.createPublicKnowledgeBase("Public Docs", "Description");

        assertThat(kb.getTenantId()).isEqualTo(0L);
        assertThat(kb.getVisibility()).isEqualTo("PUBLIC");
        assertThat(kb.getStatus()).isEqualTo("ACTIVE");
        assertThat(kb.getName()).isEqualTo("Public Docs");
    }

    @Test
    void create_shouldSetPrivateAndActive() {
        when(knowledgeBaseRepository.save(any())).thenAnswer(inv -> {
            KnowledgeBase kb = inv.getArgument(0);
            kb.setId(1L);
            return kb;
        });

        var kb = knowledgeBaseService.create(10L, "My KB", "desc");

        assertThat(kb.getTenantId()).isEqualTo(10L);
        assertThat(kb.getVisibility()).isEqualTo("PRIVATE");
        assertThat(kb.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void createDocument_shouldCreateAsyncTask() {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(1L);
        kb.setTenantId(10L);
        when(knowledgeBaseRepository.findByIdAndTenantIdAndDeleted(1L, 10L, (short) 0))
                .thenReturn(Optional.of(kb));
        when(knowledgeDocumentRepository.save(any())).thenAnswer(inv -> {
            KnowledgeDocument doc = inv.getArgument(0);
            doc.setId(100L);
            return doc;
        });
        when(asyncTaskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var doc = knowledgeBaseService.createDocument(1L, 10L, 1L, "Title", "Content");

        assertThat(doc.getSyncStatus()).isEqualTo(SyncStatus.PENDING);
        assertThat(doc.getSourceType()).isEqualTo(SourceType.MANUAL);
        verify(asyncTaskRepository).save(argThat(task ->
                "DOC_INDEX".equals(task.getTaskType()) && task.getRefId().equals(100L)));
    }

    @Test
    void reindexDocument_shouldResetStatusAndCreateTask() {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(1L);
        kb.setTenantId(10L);
        when(knowledgeBaseRepository.findByIdAndTenantIdAndDeleted(1L, 10L, (short) 0))
                .thenReturn(Optional.of(kb));

        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setId(100L);
        doc.setKnowledgeBaseId(1L);
        doc.setTenantId(10L);
        doc.setSyncStatus(SyncStatus.INDEXED);
        doc.setDeleted((short) 0);
        when(knowledgeDocumentRepository.findById(100L)).thenReturn(Optional.of(doc));
        when(knowledgeDocumentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(asyncTaskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = knowledgeBaseService.reindexDocument(1L, 10L, 1L, 100L);

        assertThat(result.getSyncStatus()).isEqualTo(SyncStatus.PENDING);
        verify(asyncTaskRepository).save(argThat(task ->
                "DOC_INDEX".equals(task.getTaskType()) && task.getRefId().equals(100L)));
    }

    @Test
    void search_ragDisabled_shouldFallbackToKeywordSearch() {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(1L);
        kb.setTenantId(10L);
        when(knowledgeBaseRepository.findByIdAndTenantIdAndDeleted(1L, 10L, (short) 0))
                .thenReturn(Optional.of(kb));
        when(aiServiceProperties.isRag()).thenReturn(false);
        when(knowledgeDocumentRepository
                .findByKnowledgeBaseIdAndDeletedOrderByUpdatedAtDesc(1L, (short) 0))
                .thenReturn(List.of());

        var result = knowledgeBaseService.search(1L, 10L, "test query", 5);

        assertThat(result).isEmpty();
    }

    @Test
    void search_keywordFallback_shouldReturnMatchingDocs() {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(1L);
        kb.setTenantId(10L);
        when(knowledgeBaseRepository.findByIdAndTenantIdAndDeleted(1L, 10L, (short) 0))
                .thenReturn(Optional.of(kb));
        when(aiServiceProperties.isRag()).thenReturn(false);

        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setId(10L);
        doc.setTitle("Relevant document");
        doc.setContent("This is a test document with relevant content for the query");
        when(knowledgeDocumentRepository
                .findByKnowledgeBaseIdAndDeletedOrderByUpdatedAtDesc(1L, (short) 0))
                .thenReturn(List.of(doc));

        var result = knowledgeBaseService.search(1L, 10L, "relevant content", 5);

        assertThat(result).isNotEmpty();
        assertThat(result.get(0)).containsEntry("documentId", 10L);
        assertThat((long) result.get(0).get("score")).isGreaterThan(0);
    }

    @Test
    void deleteDocument_shouldSoftDelete() {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(1L);
        kb.setTenantId(10L);
        when(knowledgeBaseRepository.findByIdAndTenantIdAndDeleted(1L, 10L, (short) 0))
                .thenReturn(Optional.of(kb));

        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setId(100L);
        doc.setKnowledgeBaseId(1L);
        doc.setTenantId(10L);
        doc.setDeleted((short) 0);
        when(knowledgeDocumentRepository.findById(100L)).thenReturn(Optional.of(doc));
        when(knowledgeDocumentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        knowledgeBaseService.deleteDocument(1L, 10L, 100L);

        assertThat(doc.getDeleted()).isEqualTo((short) 1);
    }
}
