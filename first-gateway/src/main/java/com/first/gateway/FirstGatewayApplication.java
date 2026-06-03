package com.first.gateway;

import com.first.gateway.config.AiServiceProperties;
import com.first.gateway.config.AuthProperties;
import com.first.gateway.config.GatewayProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({GatewayProperties.class, AuthProperties.class, AiServiceProperties.class})
@org.springframework.scheduling.annotation.EnableScheduling
public class FirstGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(FirstGatewayApplication.class, args);
    }
}