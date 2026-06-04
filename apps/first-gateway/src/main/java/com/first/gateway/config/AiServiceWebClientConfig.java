package com.first.gateway.config;

import io.netty.channel.ChannelOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class AiServiceWebClientConfig {

    @Bean
    public WebClient aiServiceWebClient(AiServiceProperties aiServiceProperties) {
        HttpClient httpClient = HttpClient.create()
            .responseTimeout(Duration.ofMillis(aiServiceProperties.getReadTimeoutMs()))
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, aiServiceProperties.getConnectTimeoutMs());

        return WebClient.builder()
            .baseUrl(aiServiceProperties.getBaseUrl())
            .defaultHeader("X-Internal-Token", aiServiceProperties.getInternalToken())
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();
    }
}