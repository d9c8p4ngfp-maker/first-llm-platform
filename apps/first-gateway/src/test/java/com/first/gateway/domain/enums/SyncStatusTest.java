package com.first.gateway.domain.enums;

import com.first.gateway.domain.entity.KnowledgeDocument;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SyncStatusTest {

    @Test
    void shouldContainFiveStatuses() {
        assertThat(SyncStatus.values())
            .containsExactly(SyncStatus.PENDING, SyncStatus.CRAWLING,
                SyncStatus.INDEXING, SyncStatus.INDEXED, SyncStatus.FAILED);
    }

    @Test
    void knowledgeDocumentSyncStatusDefaultIsPending() {
        KnowledgeDocument doc = new KnowledgeDocument();
        assertThat(doc.getSyncStatus()).isEqualTo(SyncStatus.PENDING);
    }

    @Test
    void knowledgeDocumentSetAndGetSyncStatus() {
        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setSyncStatus(SyncStatus.INDEXING);
        assertThat(doc.getSyncStatus()).isEqualTo(SyncStatus.INDEXING);
    }
}
