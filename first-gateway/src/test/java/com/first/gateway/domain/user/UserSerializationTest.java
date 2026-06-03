package com.first.gateway.domain.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.first.gateway.domain.entity.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class UserSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void userJson_excludesPasswordHash() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setPasswordHash("secret-hash");

        String json = objectMapper.writeValueAsString(user);

        assertFalse(json.contains("passwordHash"));
        assertFalse(json.contains("secret-hash"));
    }
}