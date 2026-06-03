package com.first.gateway.infra.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.first.gateway.infra.filter.FilterErrorWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimitResponseTest {

    private ObjectMapper objectMapper;
    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        handler = new GlobalExceptionHandler();
    }

    @Test
    void rpmExceeded_hasRetryAfter() throws Exception {
        RateLimitExceededException ex = new RateLimitExceededException(
            RateLimitExceededException.LimitType.RPM, 60, 100, 0);
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterErrorWriter.write(objectMapper, response, ex);

        assertEquals(429, response.getStatus());
        assertEquals("60", response.getHeader("Retry-After"));
        assertEquals("100", response.getHeader("X-RateLimit-Limit-RPM"));
        assertEquals("0", response.getHeader("X-RateLimit-Remaining-RPM"));
    }

    @Test
    void tpmExceeded_hasRetryAfter() {
        RateLimitExceededException ex = new RateLimitExceededException(
            RateLimitExceededException.LimitType.TPM, 60, 50_000, 100);

        ResponseEntity<Map<String, Object>> response = handler.handleGateway(ex);

        assertEquals(429, response.getStatusCode().value());
        assertEquals("60", response.getHeaders().getFirst("Retry-After"));
        assertEquals("50000", response.getHeaders().getFirst("X-RateLimit-Limit-TPM"));
        assertEquals("100", response.getHeaders().getFirst("X-RateLimit-Remaining-TPM"));
    }

    @Test
    void concurrentExceeded_has429() throws Exception {
        RateLimitExceededException ex = new RateLimitExceededException(
            RateLimitExceededException.LimitType.CONCURRENT, 30, 1, 0);
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterErrorWriter.write(objectMapper, response, ex);

        assertEquals(429, response.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = objectMapper.readValue(response.getContentAsString(), Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> error = (Map<String, Object>) body.get("error");
        assertEquals("rate_limit_exceeded", error.get("code"));
    }

    @Test
    void headerFormat_matchesOpenAI() throws Exception {
        RateLimitExceededException ex = new RateLimitExceededException(
            RateLimitExceededException.LimitType.RPM, 60, 120, 45);
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterErrorWriter.write(objectMapper, response, ex);

        assertNotNull(response.getHeader("X-RateLimit-Limit-RPM"));
        assertNotNull(response.getHeader("X-RateLimit-Remaining-RPM"));
        assertTrue(response.getHeader("X-RateLimit-Limit-RPM").matches("\\d+"));
        assertTrue(response.getHeader("X-RateLimit-Remaining-RPM").matches("\\d+"));
    }
}