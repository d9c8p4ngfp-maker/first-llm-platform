package com.first.gateway.config;

import io.netty.channel.ChannelOption;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient upstreamWebClient(GatewayProperties gatewayProperties) {
        ConnectionProvider provider = ConnectionProvider.builder("upstream")
            .maxConnections(200)
            .pendingAcquireMaxCount(500)
            .maxIdleTime(Duration.ofSeconds(30))
            .evictInBackground(Duration.ofSeconds(60))
            .build();

        HttpClient httpClient = HttpClient.create(provider)
            .responseTimeout(Duration.ofMillis(gatewayProperties.getTimeout().getReadMs()))
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, gatewayProperties.getTimeout().getConnectMs());

        return WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .filter((request, next) -> {
                String traceId = MDC.get("traceId");
                if (traceId != null) {
                    request = ClientRequest.from(request)
                        .header(gatewayProperties.getTrace().getHeaderName(), traceId)
                        .build();
                }
                return next.exchange(request);
            })
            .build();
    }
}
