package com.mazen.helpdesk.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(String secret, Duration expiration) {
    
        public JwtProperties {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("security.jwt.secret must be at least 32 bytes (256 bits) for HS256");
        }
    }
}
