package com.first.gateway.infra.ai.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class UpstreamConfigTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldSerializeWithSnakeCase() throws Exception {
        UpstreamConfig config = new UpstreamConfig("https://api.openai.com", "sk-xxx", "text-embedding-3-small");
        String json = mapper.writeValueAsString(config);
        assertThat(json).contains("base_url").contains("api_key").contains("model");
    }

    @Test
    void shouldDeserializeSnakeCase() throws Exception {
        String json = "{\"base_url\":\"https://api.openai.com\",\"api_key\":\"sk-xxx\",\"model\":\"text-embedding-3-small\"}";
        UpstreamConfig config = mapper.readValue(json, UpstreamConfig.class);
        assertThat(config.baseUrl()).isEqualTo("https://api.openai.com");
        assertThat(config.apiKey()).isEqualTo("sk-xxx");
        assertThat(config.model()).isEqualTo("text-embedding-3-small");
    }
}
