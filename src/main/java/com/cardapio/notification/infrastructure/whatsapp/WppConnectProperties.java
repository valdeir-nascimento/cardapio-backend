package com.cardapio.notification.infrastructure.whatsapp;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "wppconnect")
public record WppConnectProperties(
    boolean enabled,
    String session,
    String token,
    String baseUrl
) {}
