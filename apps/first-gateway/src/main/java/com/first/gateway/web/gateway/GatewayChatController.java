package com.first.gateway.web.gateway;

import com.first.gateway.infra.error.GatewayError;
import com.first.gateway.infra.error.GatewayException;
import com.first.gateway.infra.filter.GatewayRequestAttributes;
import com.first.gateway.repository.ChannelRepository;
import com.first.gateway.service.auth.AuthService;
import com.first.gateway.service.auth.RateLimitService;
import com.first.gateway.service.billing.BillingCostCalculator;
import com.first.gateway.service.channel.ChannelService;
import com.first.gateway.service.relay.RelayService;
import com.first.gateway.web.support.ChatEndpointSupport;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1")
public class GatewayChatController {

    private final RelayService relayService;
    private final ChannelService channelService;
    private final ChannelRepository channelRepository;
    private final RateLimitService rateLimitService;

    public GatewayChatController(RelayService relayService,
                                 ChannelService channelService,
                                 ChannelRepository channelRepository,
                                 RateLimitService rateLimitService) {
        this.relayService = relayService;
        this.channelService = channelService;
        this.channelRepository = channelRepository;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping(value = "/chat/completions", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_EVENT_STREAM_VALUE})
    public Object chat(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        AuthService.AuthContext auth = requireAuth(request);
        reserveTpm(auth, body, request);
        boolean stream = Boolean.TRUE.equals(body.get("stream"));
        long tpmReserved = tpmReserved(request);
        if (stream) {
            return ChatEndpointSupport.buildStreamingResponse(relayService, auth, body, tpmReserved);
        }
        return relayService.chatCompletions(auth, body, tpmReserved(request));
    }

    @PostMapping(value = "/embeddings", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> embeddings(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        AuthService.AuthContext auth = requireAuth(request);
        reserveTpm(auth, body, request);
        return relayService.embeddings(auth, body, tpmReserved(request));
    }

    @GetMapping("/models")
    public Map<String, Object> models(HttpServletRequest request) {
        requireAuth(request);
        List<Map<String, Object>> data = channelService.listEnabledModels().stream()
            .map(cm -> {
                String ownedBy = channelRepository.findById(cm.getChannelId())
                    .map(ch -> ch.getProvider() != null ? ch.getProvider() : ch.getType())
                    .orElse("first-gateway");
                return modelEntry(cm.getModelAlias() != null ? cm.getModelAlias() : cm.getModelName(), ownedBy);
            })
            .distinct()
            .toList();
        return Map.of("object", "list", "data", data);
    }

    private void reserveTpm(AuthService.AuthContext auth, Map<String, Object> body, HttpServletRequest request) {
        long tpmReserved = rateLimitService.reserveTpm(auth.apiKey(), BillingCostCalculator.estimateTokens(body));
        request.setAttribute(GatewayRequestAttributes.TPM_RESERVED, tpmReserved);
    }

    private static long tpmReserved(HttpServletRequest request) {
        Object value = request.getAttribute(GatewayRequestAttributes.TPM_RESERVED);
        if (value instanceof Number number) {
            return number.longValue();
        }
        return 0L;
    }

    private static AuthService.AuthContext requireAuth(HttpServletRequest request) {
        Object value = request.getAttribute(GatewayRequestAttributes.AUTH_CONTEXT);
        if (value instanceof AuthService.AuthContext authContext) {
            return authContext;
        }
        throw new GatewayException(GatewayError.INVALID_API_KEY);
    }

    private static Map<String, Object> modelEntry(String id, String ownedBy) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("id", id);
        entry.put("object", "model");
        entry.put("owned_by", ownedBy);
        return entry;
    }
}
