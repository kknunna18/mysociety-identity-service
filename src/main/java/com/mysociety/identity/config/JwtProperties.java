package com.mysociety.identity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "identity.jwt")
public record JwtProperties(String secret, Duration accessTtl, Duration refreshTtl) {
    public JwtProperties {
        if (secret == null || secret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("identity.jwt.secret must be at least 256 bits");
        }
    }
}
