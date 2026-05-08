package com.cardapio.ordering.infrastructure.qr;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "qr.storage")
public record FilesystemQrProperties(
    String type,
    String path,
    String publicBaseUrl,
    String signingSecret
) {}
