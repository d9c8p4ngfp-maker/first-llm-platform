import fs from 'node:fs'

const content = `package com.first.gateway.web.workspace;

import com.first.gateway.infra.error.GatewayError;
import com.first.gateway.infra.error.GatewayException;
import com.first.gateway.infra.filter.GatewayRequestAttributes;
import com.first.gateway.service.auth.AuthService;
import com.first.gateway.service.auth.RateLimitService;
import com.first.gateway.service.auth.admin.AdminPrincipal;
import com.first.gateway.service.billing.BillingCostCalculator;
import com.first.gateway.service.relay.RelayService;
import com.first.gateway.web.workspace.support.WorkspaceAccess;
import com.first.gateway.web.workspace.support.WorkspaceRequest;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class WorkspaceChatController {

    private final RelayService relayService;
    private final AuthService authService;
    private final RateLimitService rateLimitService;

    public WorkspaceChatController(RelayService relayService,
                                   AuthService authService,
                                   RateLimitService rateLimitService) {
        this.relayService = relayService;
        this.authService = authService;
        this.rateLimitService = rateLimitService;
    }

    @PostMapping(value = "/chat/completions", produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_EVENT_STREAM_VALUE})
    public Object chat(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        AdminPrincipal principal = WorkspaceAccess.requirePrincipal(WorkspaceRequest.principal(request));
        AuthService.AuthContext auth = authService.resolveContextForUser(principal.userId(), principal.tenantId());
        reserveTpm(auth, body, request);
        boolean stream = Boolean.TRUE.equals(body.get("stream"));
        long tpmReserved = tpmReserved(request);
        if (stream) {
            return new StreamingResponseBody() {
                @Override
                public void writeTo(OutputStream outputStream) throws IOException {
                    relayService.chatCompletionsStream(auth, body, tpmReserved, chunk -> {
                        try {
                            outputStream.write(chunk.getBytes(StandardCharsets.UTF_8));
                            outputStream.flush();
                        } catch (IOException ex) {
                            throw new GatewayException(GatewayError.INTERNAL_ERROR);
                        }
                    });
                }
            };
        }
        return relayService.chatCompletions(auth, body, tpmReserved);
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
}
`

fs.writeFileSync('D:/first/first-gateway/src/main/java/com/first/gateway/web/workspace/WorkspaceChatController.java', content, 'utf8')
console.log('WorkspaceChatController written')
