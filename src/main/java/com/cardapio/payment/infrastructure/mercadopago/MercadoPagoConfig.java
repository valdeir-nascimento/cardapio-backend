package com.cardapio.payment.infrastructure.mercadopago;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "mercadopago.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
class MercadoPagoConfig {

    private final MercadoPagoProperties props;

    @PostConstruct
    void init() {
        com.mercadopago.MercadoPagoConfig.setAccessToken(props.accessToken());
        log.info("Mercado Pago SDK initialized");
    }
}
