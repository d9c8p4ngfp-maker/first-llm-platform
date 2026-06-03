package com.first.gateway.service.auth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.first.gateway.domain.entity.ApiKey;
import com.first.gateway.infra.error.GatewayError;
import com.first.gateway.infra.error.GatewayException;
import com.first.gateway.infra.web.IpWhitelist;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class ApiKeyPolicyService {

    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};

    private final ObjectMapper objectMapper;

    public ApiKeyPolicyService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void assertModelAllowed(ApiKey apiKey, String model) {
        List<String> allowed = parseAllowedModels(apiKey.getAllowedModels());
        if (allowed.isEmpty()) {
            return;
        }
        if (!allowed.contains(model)) {
            throw new GatewayException(GatewayError.MODEL_NOT_ALLOWED);
        }
    }

    public void assertIpAllowed(ApiKey apiKey, String clientIp) {
        List<String> allowedIps = parseAllowedIps(apiKey.getSecurityConfig());
        if (allowedIps.isEmpty()) {
            return;
        }
        if (!IpWhitelist.matches(clientIp, allowedIps)) {
            throw new GatewayException(GatewayError.IP_NOT_ALLOWED);
        }
    }

    List<String> parseAllowedModels(String raw) {
        if (raw == null || raw.isBlank()) {
            return Collections.emptyList();
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("[")) {
            try {
                List<String> values = objectMapper.readValue(trimmed, STRING_LIST);
                return values.stream().filter(v -> v != null && !v.isBlank()).map(String::trim).toList();
            } catch (Exception ex) {
                throw new GatewayException(GatewayError.INTERNAL_ERROR);
            }
        }
        return List.of(trimmed.split(",")).stream()
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
    }

    List<String> parseAllowedIps(String securityConfig) {
        if (securityConfig == null || securityConfig.isBlank()) {
            return Collections.emptyList();
        }
        try {
            JsonNode root = objectMapper.readTree(securityConfig);
            JsonNode ips = root.get("allowed_ips");
            if (ips == null || !ips.isArray() || ips.isEmpty()) {
                return Collections.emptyList();
            }
            return objectMapper.convertValue(ips, STRING_LIST).stream()
                .filter(v -> v != null && !v.isBlank())
                .map(String::trim)
                .toList();
        } catch (Exception ex) {
            throw new GatewayException(GatewayError.INTERNAL_ERROR);
        }
    }
}
