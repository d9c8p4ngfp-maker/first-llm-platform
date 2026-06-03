package com.first.gateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.first.gateway.relay.support.UsageParser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RelaySupportConfig {

    @Bean
    UsageParser usageParser(ObjectMapper objectMapper) {
        return new UsageParser(objectMapper);
    }
}
