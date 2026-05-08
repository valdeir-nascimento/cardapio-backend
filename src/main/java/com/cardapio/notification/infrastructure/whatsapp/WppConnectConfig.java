package com.cardapio.notification.infrastructure.whatsapp;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

@Configuration
@ConditionalOnProperty(name = "wppconnect.enabled", havingValue = "true")
class WppConnectConfig {

    @Bean("wppconnectRestClient")
    RestClient wppconnectRestClient(WppConnectProperties props) {
        return RestClient.builder()
            .baseUrl(props.baseUrl())
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + props.token())
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }
}
