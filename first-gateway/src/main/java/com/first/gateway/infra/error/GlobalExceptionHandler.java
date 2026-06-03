package com.first.gateway.infra.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(GatewayException.class)
    public ResponseEntity<Map<String, Object>> handleGateway(GatewayException ex) {
        GatewayError error = ex.getError();
        String logMessage = ex.getInternalDetail() != null ? ex.getInternalDetail() : ex.getMessage();
        if (error.getHttpStatus().is5xxServerError()) {
            log.error("[{}] {}", MDC.get("traceId"), logMessage, ex);
        } else {
            log.warn("[{}] {} - {}", MDC.get("traceId"), error.getCode(), logMessage);
        }
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(error.getHttpStatus());
        if (ex instanceof RateLimitExceededException rateLimitEx) {
            applyRateLimitHeaders(builder, rateLimitEx);
        }
        return builder.body(ErrorResponses.body(ex));
    }

    private static void applyRateLimitHeaders(ResponseEntity.BodyBuilder builder,
                                              RateLimitExceededException ex) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Retry-After", String.valueOf(ex.getRetryAfterSeconds()));
        if (ex.getLimit() != null) {
            switch (ex.getLimitType()) {
                case RPM -> {
                    headers.add("X-RateLimit-Limit-RPM", String.valueOf(ex.getLimit()));
                    if (ex.getRemaining() != null) {
                        headers.add("X-RateLimit-Remaining-RPM", String.valueOf(ex.getRemaining()));
                    }
                }
                case TPM -> {
                    headers.add("X-RateLimit-Limit-TPM", String.valueOf(ex.getLimit()));
                    if (ex.getRemaining() != null) {
                        headers.add("X-RateLimit-Remaining-TPM", String.valueOf(ex.getRemaining()));
                    }
                }
                case CONCURRENT -> headers.add("X-RateLimit-Limit-Concurrent", String.valueOf(ex.getLimit()));
            }
        }
        builder.headers(headers);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(f -> f.getDefaultMessage() != null ? f.getDefaultMessage() : GatewayError.INVALID_REQUEST.getDefaultMessage())
            .orElse(GatewayError.INVALID_REQUEST.getDefaultMessage());
        return ResponseEntity.status(GatewayError.INVALID_REQUEST.getHttpStatus())
            .body(ErrorResponses.body(GatewayError.INVALID_REQUEST, message));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException ex) {
        String detail = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
        String message = GatewayError.INVALID_REQUEST.getDefaultMessage();
        if (detail != null) {
            if (detail.contains("user.username")) {
                message = "用户名已被占用";
            } else if (detail.contains("user.email")) {
                message = "邮箱已被注册";
            }
        }
        log.warn("[{}] Data integrity violation: {}", MDC.get("traceId"), detail);
        return ResponseEntity.status(GatewayError.INVALID_REQUEST.getHttpStatus())
            .body(ErrorResponses.body(GatewayError.INVALID_REQUEST, message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAll(Exception ex) {
        log.error("[{}] Unexpected error", MDC.get("traceId"), ex);
        return ResponseEntity.status(GatewayError.INTERNAL_ERROR.getHttpStatus())
            .body(ErrorResponses.body(GatewayError.INTERNAL_ERROR, null));
    }
}
