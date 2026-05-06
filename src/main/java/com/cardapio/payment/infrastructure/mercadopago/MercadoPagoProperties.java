package com.cardapio.payment.infrastructure.mercadopago;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mercadopago")
public record MercadoPagoProperties(
    boolean enabled,
    String accessToken,
    String webhookSecret,
    String notificationUrl
) {}
