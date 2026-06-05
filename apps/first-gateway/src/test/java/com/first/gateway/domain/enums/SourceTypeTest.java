package com.first.gateway.domain.enums;

import com.first.gateway.domain.entity.KnowledgeDocument;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SourceTypeTest {

    @Test
    void shouldContainThreeValues() {
        assertThat(SourceType.values())
            .containsExactly(SourceType.MANUAL, SourceType.FILE, SourceType.URL);
    }

    @Test
    void knowledgeDocumentDefaultSourceTypeIsManual() {
        KnowledgeDocument doc = new KnowledgeDocument();
        assertThat(doc.getSourceType()).isEqualTo(SourceType.MANUAL);
    }
}
