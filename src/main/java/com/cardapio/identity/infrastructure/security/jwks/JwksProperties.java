package com.cardapio.identity.infrastructure.security.jwks;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "jwks")
public record JwksProperties(Duration cacheTtl, int fetchTimeoutMs) {
    public JwksProperties {
        if (cacheTtl == null) cacheTtl = Duration.ofHours(1);
        if (fetchTimeoutMs <= 0) fetchTimeoutMs = 5000;
    }
}
