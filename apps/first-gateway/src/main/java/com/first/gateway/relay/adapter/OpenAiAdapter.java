package com.first.gateway.relay.adapter;

import com.first.gateway.domain.entity.Channel;
import com.first.gateway.infra.crypto.ChannelKeyCrypto;
import com.first.gateway.infra.error.GatewayError;
import com.first.gateway.infra.error.GatewayException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

@Component
public class OpenAiAdapter implements LlmAdapter {

    private final WebClient upstreamWebClient;
    private final ChannelKeyCrypto channelKeyCrypto;

    public OpenAiAdapter(WebClient upstreamWebClient, ChannelKeyCrypto channelKeyCrypto) {
        this.upstreamWebClient = upstreamWebClient;
        this.channelKeyCrypto = channelKeyCrypto;
    }

    @Override
    public String getType() {
        return "openai";
    }

    @Override
    public Map<String, Object> chat(Channel channel, Map<String, Object> request) {
        Map<String, Object> body = new HashMap<>(request);
        body.put("stream", false);
        body.remove("stream_options");
        try {
            return postChat(channel)
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::toGatewayError)
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .blockOptional(Duration.ofMinutes(5))
                .orElseThrow(() -> GatewayException.withInternal(
                    GatewayError.UPSTREAM_ERROR, "upstream empty response"));
        } catch (GatewayException ex) {
            throw ex;
        } catch (WebClientRequestException ex) {
            throw GatewayException.withInternal(GatewayError.UPSTREAM_TIMEOUT, ex.getMessage());
        } catch (Exception ex) {
            throw GatewayException.withInternal(GatewayError.UPSTREAM_ERROR, ex.getMessage());
        }
    }

    public void chatStream(Channel channel, Map<String, Object> request, Consumer<String> chunkConsumer) {
        Map<String, Object> body = new HashMap<>(request);
        body.put("stream", true);
        body.put("stream_options", Map.of("include_usage", true));
        try {
            var flux = postChat(channel)
                .bodyValue(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::toGatewayError)
                .bodyToFlux(org.springframework.core.io.buffer.DataBuffer.class);

            StringBuilder pending = new StringBuilder();
            flux.doOnNext(buffer -> {
                pending.append(buffer.toString(StandardCharsets.UTF_8));
                org.springframework.core.io.buffer.DataBufferUtils.release(buffer);
                flushLines(pending, chunkConsumer);
            }).doOnComplete(() -> {
                if (!pending.isEmpty()) {
                    chunkConsumer.accept(pending.toString());
                }
            }).blockLast(Duration.ofMinutes(10));
        } catch (GatewayException ex) {
            throw ex;
        } catch (WebClientRequestException ex) {
            throw GatewayException.withInternal(GatewayError.UPSTREAM_TIMEOUT, ex.getMessage());
        } catch (Exception ex) {
            throw GatewayException.withInternal(GatewayError.UPSTREAM_ERROR, ex.getMessage());
        }
    }

    private static void flushLines(StringBuilder pending, Consumer<String> chunkConsumer) {
        int newline;
        while ((newline = pending.indexOf("\n")) >= 0) {
            String line = pending.substring(0, newline + 1);
            pending.delete(0, newline + 1);
            chunkConsumer.accept(line);
        }
    }

    private Mono<? extends Throwable> toGatewayError(org.springframework.web.reactive.function.client.ClientResponse response) {
        int status = response.statusCode().value();
        return response.bodyToMono(String.class)
            .defaultIfEmpty("")
            .flatMap(body -> Mono.error(GatewayException.withInternal(
                mapUpstreamStatus(status),
                "upstream " + status + ": " + trimBody(body))));
    }

    static GatewayError mapUpstreamStatus(int status) {
        if (status == HttpStatus.TOO_MANY_REQUESTS.value()) {
            return GatewayError.RATE_LIMIT_EXCEEDED;
        }
        return GatewayError.UPSTREAM_ERROR;
    }

    private static String trimBody(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }
        return body.length() > 500 ? body.substring(0, 500) : body;
    }

    protected WebClient.RequestBodySpec postChat(Channel channel) {
        String apiKey = channelKeyCrypto.decrypt(channel.getApiKeyEncrypted());
        return upstreamWebClient.post()
            .uri(resolveChatUri(channel))
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json");
    }

    public Map<String, Object> embed(Channel channel, Map<String, Object> request) {
        try {
            return postEmbeddings(channel)
                .bodyValue(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, this::toGatewayError)
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .blockOptional(Duration.ofMinutes(2))
                .orElseThrow(() -> GatewayException.withInternal(
                    GatewayError.UPSTREAM_ERROR, "upstream empty response"));
        } catch (GatewayException ex) {
            throw ex;
        } catch (WebClientRequestException ex) {
            throw GatewayException.withInternal(GatewayError.UPSTREAM_TIMEOUT, ex.getMessage());
        } catch (Exception ex) {
            throw GatewayException.withInternal(GatewayError.UPSTREAM_ERROR, ex.getMessage());
        }
    }

    private WebClient.RequestBodySpec postEmbeddings(Channel channel) {
        String apiKey = channelKeyCrypto.decrypt(channel.getApiKeyEncrypted());
        return upstreamWebClient.post()
            .uri(resolveEmbeddingsUri(channel))
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json");
    }

    private static String resolveEmbeddingsUri(Channel channel) {
        String base = channel.getBaseUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/v1/embeddings";
    }

    private static String resolveChatUri(Channel channel) {
        String base = channel.getBaseUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/v1/chat/completions";
    }
}
