package com.first.gateway.infra.error;

import org.springframework.http.HttpStatus;

public enum GatewayError {

    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "invalid_request", "请求参数不合法"),
    MODEL_NOT_FOUND(HttpStatus.BAD_REQUEST, "model_not_found", "请求的模型不可用"),
    INVALID_API_KEY(HttpStatus.UNAUTHORIZED, "invalid_api_key", "API key 无效或不存在"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "invalid_credentials", "用户名或密码错误"),
    INVALID_JWT(HttpStatus.UNAUTHORIZED, "invalid_jwt", "JWT 无效或已过期"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "token_expired", "令牌已过期"),
    TOKEN_REVOKED(HttpStatus.UNAUTHORIZED, "token_revoked", "令牌已吊销"),
    INSUFFICIENT_QUOTA(HttpStatus.PAYMENT_REQUIRED, "insufficient_quota", "额度不足"),
    TOKEN_QUOTA_EXCEEDED(HttpStatus.PAYMENT_REQUIRED, "token_quota_exceeded", "令牌额度已用完"),
    MODEL_NOT_ALLOWED(HttpStatus.FORBIDDEN, "model_not_allowed", "令牌不允许使用此模型"),
    IP_NOT_ALLOWED(HttpStatus.FORBIDDEN, "ip_not_allowed", "IP 不在白名单内"),
    USER_BANNED(HttpStatus.FORBIDDEN, "user_banned", "用户已被禁用"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "access_denied", "无权访问"),
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "rate_limit_exceeded", "请求频率超限"),
    UPSTREAM_ERROR(HttpStatus.BAD_GATEWAY, "upstream_error", "上游服务异常"),
    UPSTREAM_TIMEOUT(HttpStatus.BAD_GATEWAY, "upstream_timeout", "上游服务超时"),
    NO_AVAILABLE_CHANNEL(HttpStatus.SERVICE_UNAVAILABLE, "no_available_channel", "无可用渠道"),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "service_unavailable", "服务暂不可用"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "internal_error", "系统内部错误");

    private final HttpStatus httpStatus;
    private final String code;
    private final String defaultMessage;

    GatewayError(HttpStatus httpStatus, String code, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
