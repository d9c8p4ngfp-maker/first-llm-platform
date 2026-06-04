package com.first.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@Profile("!dev & !test")
public class SecurityBootValidator implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SecurityBootValidator.class);

    private static final List<String> FORBIDDEN = Arrays.asList(
        "must-change-in-production",
        "must-change-in-production-use-env-var-32b-min",
        "must-change-this-token",
        "changeit",
        "changeme",
        "secret",
        "password"
    );

    @Value("${gateway.crypto.channel-key-secret}")
    private String channelKeySecret;

    @Value("${auth.jwt.secret}")
    private String jwtSecret;

    @Override
    public void run(String... args) {
        check("gateway.crypto.channel-key-secret", channelKeySecret);
        check("auth.jwt.secret", jwtSecret, 32);
    }

    private void check(String name, String value) {
        check(name, value, 16);
    }

    private void check(String name, String value, int minLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Secret " + name + " must not be empty in production");
        }
        if (value.length() < minLength) {
            throw new IllegalStateException("Secret " + name + " must be at least " + minLength + " chars");
        }
        String lower = value.toLowerCase();
        for (String forbidden : FORBIDDEN) {
            if (lower.contains(forbidden)) {
                throw new IllegalStateException(
                    "Secret " + name + " contains forbidden expression '" + forbidden + "'. "
                    + "Please set a secure value via environment variable.");
            }
        }
    }
}
