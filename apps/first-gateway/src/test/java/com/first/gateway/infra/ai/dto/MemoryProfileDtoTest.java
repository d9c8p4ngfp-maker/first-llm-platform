package com.first.gateway.infra.ai.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class MemoryProfileDtoTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void shouldSerializeMemoryExtractRequest() throws Exception {
        var req = new MemoryExtractRequest(List.of(new ChatMessage("user", "hello")),
            List.of(new ExistingMemoryRef(1L, "FACT", "remember this")),
            new ModelConfig("gpt-4", new ModelParams(0.7, 4000), null),
            new UpstreamConfig("https://api.com", "key", "gpt-4"));
        String json = mapper.writeValueAsString(req);
        assertThat(json).contains("conversation_segment").contains("existing_memories").contains("upstream");
    }

    @Test
    void shouldDeserializeProfileSynthesizeResponse() throws Exception {
        String json = "{\"profile\":{\"ai_summary\":\"summary\",\"ai_tags\":\"tag1,tag2\",\"ai_personality\":{\"mbti\":\"INTJ\",\"mbti_label\":\"Architect\",\"zodiac\":\"Aries\"},\"ai_system_prompt\":\"prompt\"}}";
        ProfileSynthesizeResponse resp = mapper.readValue(json, ProfileSynthesizeResponse.class);
        assertThat(resp.profile().aiSummary()).isEqualTo("summary");
        assertThat(resp.profile().aiPersonality().mbti()).isEqualTo("INTJ");
    }
}
