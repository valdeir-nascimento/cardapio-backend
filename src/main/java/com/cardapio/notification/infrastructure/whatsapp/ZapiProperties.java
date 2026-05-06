package com.cardapio.notification.infrastructure.whatsapp;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "zapi")
public record ZapiProperties(
    boolean enabled,
    String instanceId,
    String token,
    String clientToken,
    String baseUrl
) {}
