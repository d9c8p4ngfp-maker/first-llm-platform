package com.first.gateway;

import com.first.gateway.support.RedisIntegrationSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev")
class FirstGatewayApplicationTests extends RedisIntegrationSupport {
    @Test
    void contextLoads() {}
}