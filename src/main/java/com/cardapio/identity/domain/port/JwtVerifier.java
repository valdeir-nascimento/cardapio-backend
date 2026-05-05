package com.cardapio.identity.domain.port;

import com.cardapio.identity.domain.model.Audience;
import com.cardapio.identity.domain.model.Role;

import java.util.Set;
import java.util.UUID;

public interface JwtVerifier {
    VerifiedJwt verify(String token);

    record VerifiedJwt(UUID subject, Audience audience, Set<Role> roles) {}
}
