package com.cardapio.identity.domain.port;

import com.cardapio.identity.domain.model.Audience;
import com.cardapio.identity.domain.model.Role;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public interface JwtIssuer {
    IssuedJwt issueAccessToken(UUID subject, Audience audience, Set<Role> roles);
    String generateOpaqueRefreshToken();   // returns the raw refresh token (random URL-safe string)
    Instant accessTokenExpiry(Instant now);
    Instant refreshTokenExpiry(Instant now);

    record IssuedJwt(String token, Instant expiresAt) {}
}
