package com.first.gateway.web.workspace;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.first.gateway.web.admin.dto.LoginRequest;
import com.first.gateway.web.admin.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class WorkspaceAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void register_apiV1Path_createsUserAndReturnsToken() throws Exception {
        String username = "user_" + System.currentTimeMillis();
        RegisterRequest body = new RegisterRequest(username, "password123", "test@example.com");
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.access_token").isNotEmpty())
            .andExpect(jsonPath("$.user.username").value(username));
    }

    @Test
    void registerEnabled_returnsTrueByDefault() throws Exception {
        mockMvc.perform(get("/api/v1/auth/register-enabled"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    void login_apiV1Path_returnsAccessToken() throws Exception {
        LoginRequest body = new LoginRequest("admin", "admin123");
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.access_token").isNotEmpty())
            .andExpect(jsonPath("$.user.username").value("admin"));
    }

    @Test
    void channels_requiresJwt() throws Exception {
        mockMvc.perform(get("/api/v1/channels"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void channels_withJwt_returnsUserOwnedChannels() throws Exception {
        LoginRequest body = new LoginRequest("admin", "admin123");
        String response = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        String token = objectMapper.readTree(response).get("access_token").asText();

        mockMvc.perform(get("/api/v1/channels")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").value("DeepSeek Test"));
    }

    @Test
    void dashboard_realtime_withJwt_returnsProfileAndStats() throws Exception {
        LoginRequest body = new LoginRequest("admin", "admin123");
        String response = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
        String token = objectMapper.readTree(response).get("access_token").asText();

        mockMvc.perform(get("/api/v1/dashboard/realtime")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.profile_summary.nickname").value("admin"))
            .andExpect(jsonPath("$.today_stats.requests").isNumber());
    }

    @Test
    void knowledge_bases_crud_withJwt() throws Exception {
        LoginRequest body = new LoginRequest("admin", "admin123");
        String response = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(response).get("access_token").asText();

        String created = mockMvc.perform(post("/api/v1/knowledge-bases")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Test KB\",\"description\":\"demo\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Test KB"))
            .andReturn().getResponse().getContentAsString();
        long id = objectMapper.readTree(created).get("id").asLong();

        mockMvc.perform(get("/api/v1/knowledge-bases")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").exists());

        mockMvc.perform(get("/api/v1/models")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].name").exists());

        mockMvc.perform(get("/api/v1/user-profiles/me")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nickname").value("admin"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/knowledge-bases/" + id)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
    }
}