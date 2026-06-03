package com.first.gateway.service.monitor;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiRequestLoggerTest {

    private ApiRequestLogger apiRequestLogger;
    private ListAppender<ILoggingEvent> appender;
    private Logger logger;

    @BeforeEach
    void setUp() {
        apiRequestLogger = new ApiRequestLogger(new ObjectMapper());
        logger = (Logger) LoggerFactory.getLogger(ApiRequestLogger.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
    }

    @Test
    void log_containsRequiredFields() {
        apiRequestLogger.log("req-1", "gpt-4", "sk-abcd", 10, 20, 30, 150, "success", "hello");

        String message = appender.list.getFirst().getFormattedMessage();
        assertTrue(message.contains("\"type\":\"api_request\""));
        assertTrue(message.contains("\"request_id\":\"req-1\""));
        assertTrue(message.contains("\"model\":\"gpt-4\""));
        assertTrue(message.contains("\"tokens\":30"));
        assertTrue(message.contains("\"latency_ms\":150"));
    }

    @Test
    void log_apiKeyMasked() {
        apiRequestLogger.log("req-2", "gpt-4", "sk-7hG3", 1, 1, 2, 10, "success", null);

        String message = appender.list.getFirst().getFormattedMessage();
        assertTrue(message.contains("\"api_key_prefix\":\"sk-7hG3\""));
        assertFalse(message.contains("sk-7hG3-secret-full-key"));
    }

    @Test
    void log_promptTruncated() {
        String longPrompt = "x".repeat(150);

        apiRequestLogger.log("req-3", "gpt-4", "sk-abcd", 1, 1, 2, 10, "success", longPrompt);

        String message = appender.list.getFirst().getFormattedMessage();
        assertTrue(message.contains("\"prompt_preview\":\"" + "x".repeat(100) + "\""));
        assertFalse(message.contains("x".repeat(101)));
    }
}