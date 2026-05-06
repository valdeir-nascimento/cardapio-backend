package com.cardapio.notification.infrastructure.email;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "resend")
public record ResendProperties(
    boolean enabled,
    String apiKey,
    String from,
    String baseUrl
) {}
