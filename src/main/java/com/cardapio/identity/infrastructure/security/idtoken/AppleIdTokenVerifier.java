package com.cardapio.identity.infrastructure.security.idtoken;

import com.cardapio.identity.domain.model.SocialProvider;
import com.cardapio.identity.infrastructure.security.jwks.CachedJwksProvider;

import java.util.Set;

public class AppleIdTokenVerifier extends AbstractIdTokenVerifier {

    public AppleIdTokenVerifier(CachedJwksProvider jwks, OAuthProperties.Provider config) {
        super(jwks, config.jwksUri(), Set.of(config.issuer()), config.clientId());
    }

    @Override
    public SocialProvider provider() { return SocialProvider.APPLE; }
}
