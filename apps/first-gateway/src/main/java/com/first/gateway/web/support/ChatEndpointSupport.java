package com.first.gateway.web.support;

import com.first.gateway.service.auth.AuthService;
import com.first.gateway.service.relay.RelayService;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class ChatEndpointSupport {

    private ChatEndpointSupport() {}

    public static StreamingResponseBody buildStreamingResponse(
            RelayService relayService, AuthService.AuthContext auth,
            Map<String, Object> body, long tpmReserved) {
        return outputStream -> {
            var writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
            relayService.chatCompletionsStream(auth, body, tpmReserved, chunk -> {
                try {
                    writer.write("data: " + chunk + "\n\n");
                    writer.flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            try {
                writer.write("data: [DONE]\n\n");
                writer.flush();
            } catch (IOException e) {
                // stream closed by client, safe to ignore
            }
        };
    }
}
