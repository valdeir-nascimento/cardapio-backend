package com.cardapio.identity.infrastructure.security.idtoken;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "oauth")
public record OAuthProperties(Provider google, Provider apple) {

    public record Provider(boolean enabled, String clientId, String issuer, String jwksUri) {}
}
