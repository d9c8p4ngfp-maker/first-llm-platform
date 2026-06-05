package com.first.gateway.infra.ai.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ChatDtoTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldSerializeChatRequest() throws Exception {
        var req = new ChatRequest("gpt-4", List.of(new ChatMessage("user", "hello")),
            false, new ModelParams(0.7, 4000), null, List.of(), List.of(), null,
            new UpstreamConfig("https://api.com", "key", "gpt-4"));
        String json = mapper.writeValueAsString(req);
        assertThat(json).contains("model").contains("messages").contains("model_params");
    }

    @Test
    void shouldRoundTripUserProfileContext() throws Exception {
        var ctx = new UserProfileContext("prompt text", List.of("tag1", "tag2"));
        String json = mapper.writeValueAsString(ctx);
        UserProfileContext back = mapper.readValue(json, UserProfileContext.class);
        assertThat(back.aiSystemPrompt()).isEqualTo("prompt text");
        assertThat(back.aiTags()).containsExactly("tag1", "tag2");
    }
}
