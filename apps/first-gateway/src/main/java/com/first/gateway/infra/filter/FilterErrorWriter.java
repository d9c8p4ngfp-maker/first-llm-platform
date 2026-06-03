package com.first.gateway.infra.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.first.gateway.infra.error.ErrorResponses;
import com.first.gateway.infra.error.GatewayException;
import com.first.gateway.infra.error.RateLimitExceededException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.util.Map;

public final class FilterErrorWriter {

    private FilterErrorWriter() {}

    public static void write(ObjectMapper objectMapper,
                             HttpServletResponse response,
                             GatewayException ex) throws IOException {
        response.setStatus(ex.getError().getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        if (ex instanceof RateLimitExceededException rateLimitEx) {
            applyRateLimitHeaders(response, rateLimitEx);
        }
        Map<String, Object> body = ErrorResponses.body(ex);
        objectMapper.writeValue(response.getOutputStream(), body);
    }

    private static void applyRateLimitHeaders(HttpServletResponse response, RateLimitExceededException ex) {
        response.setHeader("Retry-After", String.valueOf(ex.getRetryAfterSeconds()));
        if (ex.getLimit() != null) {
            switch (ex.getLimitType()) {
                case RPM -> {
                    response.setHeader("X-RateLimit-Limit-RPM", String.valueOf(ex.getLimit()));
                    if (ex.getRemaining() != null) {
                        response.setHeader("X-RateLimit-Remaining-RPM", String.valueOf(ex.getRemaining()));
                    }
                }
                case TPM -> {
                    response.setHeader("X-RateLimit-Limit-TPM", String.valueOf(ex.getLimit()));
                    if (ex.getRemaining() != null) {
                        response.setHeader("X-RateLimit-Remaining-TPM", String.valueOf(ex.getRemaining()));
                    }
                }
                case CONCURRENT -> response.setHeader("X-RateLimit-Limit-Concurrent", String.valueOf(ex.getLimit()));
            }
        }
    }
}
