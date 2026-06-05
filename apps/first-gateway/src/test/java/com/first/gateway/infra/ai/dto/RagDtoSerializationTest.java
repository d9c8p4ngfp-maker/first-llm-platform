package com.first.gateway.infra.ai.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

class RagDtoSerializationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldSerializeRagIndexRequest() throws Exception {
        var req = new RagIndexRequest(1L, 10L, "hello", null, "TEXT", "text-embedding-3-small",
            new UpstreamConfig("https://api.openai.com", "sk-xxx", "text-embedding-3-small"));
        String json = mapper.writeValueAsString(req);
        assertThat(json).contains("document_id").contains("knowledge_base_id").contains("upstream");
    }

    @Test
    void shouldDeserializeRagQueryResponse() throws Exception {
        String json = "{\"chunks\":[{\"document_id\":1,\"knowledge_base_id\":10,\"content\":\"test\",\"score\":0.95,\"metadata\":{}}]}";
        RagQueryResponse resp = mapper.readValue(json, RagQueryResponse.class);
        assertThat(resp.chunks()).hasSize(1);
        assertThat(resp.chunks().getFirst().content()).isEqualTo("test");
        assertThat(resp.chunks().getFirst().score()).isEqualTo(0.95);
    }

    @Test
    void shouldRoundTripRagChunkResult() throws Exception {
        var chunk = new RagChunkResult(1L, 10L, "hello world", 0.88, Map.of("key", "value"));
        String json = mapper.writeValueAsString(chunk);
        RagChunkResult back = mapper.readValue(json, RagChunkResult.class);
        assertThat(back.documentId()).isEqualTo(1L);
        assertThat(back.content()).isEqualTo("hello world");
        assertThat(back.score()).isEqualTo(0.88);
    }
}
