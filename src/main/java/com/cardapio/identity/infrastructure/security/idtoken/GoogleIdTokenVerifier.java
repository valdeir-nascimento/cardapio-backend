package com.cardapio.identity.infrastructure.security.idtoken;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.cardapio.identity.domain.model.SocialProvider;
import com.cardapio.identity.infrastructure.security.jwks.CachedJwksProvider;

import java.util.Set;

import static com.cardapio.identity.domain.port.IdTokenVerifier.InvalidIdTokenException.Reason.EMAIL_NOT_VERIFIED;

public class GoogleIdTokenVerifier extends AbstractIdTokenVerifier {

    public GoogleIdTokenVerifier(CachedJwksProvider jwks, OAuthProperties.Provider config) {
        super(jwks, config.jwksUri(),
            Set.of(config.issuer(), "accounts.google.com"),
            config.clientId());
    }

    @Override
    public SocialProvider provider() { return SocialProvider.GOOGLE; }

    @Override
    protected void afterVerify(DecodedJWT decoded) {
        Boolean emailVerified = decoded.getClaim("email_verified").asBoolean();
        if (emailVerified == null || !emailVerified) {
            throw new InvalidIdTokenException(EMAIL_NOT_VERIFIED, "google email not verified");
        }
    }
}
