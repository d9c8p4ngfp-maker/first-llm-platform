package com.first.gateway.infra.ai.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CrawlAndEmbedDtoTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldSerializeCrawlAndIndexRequest() throws Exception {
        var req = new CrawlAndIndexRequest("https://example.com", 10L, 1L, "text-embedding-3-small",
            new UpstreamConfig("https://api.openai.com", "sk-xxx", "text-embedding-3-small"));
        String json = mapper.writeValueAsString(req);
        assertThat(json).contains("url").contains("knowledge_base_id").contains("document_id");
    }

    @Test
    void shouldDeserializeEmbedResponse() throws Exception {
        String json = "{\"embeddings\":[[0.1,0.2,0.3]],\"model\":\"text-embedding-3-small\"}";
        EmbedResponse resp = mapper.readValue(json, EmbedResponse.class);
        assertThat(resp.embeddings()).hasSize(1);
        assertThat(resp.embeddings().getFirst()).containsExactly(0.1f, 0.2f, 0.3f);
        assertThat(resp.model()).isEqualTo("text-embedding-3-small");
    }
}
