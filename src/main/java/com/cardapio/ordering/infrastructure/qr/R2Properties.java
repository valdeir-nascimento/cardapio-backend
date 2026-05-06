package com.cardapio.ordering.infrastructure.qr;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "r2")
public record R2Properties(
    String accessKeyId,
    String secretAccessKey,
    String endpoint,
    String bucket,
    String region
) {
    public R2Properties {
        if (region == null || region.isBlank()) region = "auto";
    }
}
