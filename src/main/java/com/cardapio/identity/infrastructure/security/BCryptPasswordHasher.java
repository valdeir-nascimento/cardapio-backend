package com.cardapio.identity.infrastructure.security;

import com.cardapio.identity.domain.model.HashedPassword;
import com.cardapio.identity.domain.model.RawPassword;
import com.cardapio.identity.domain.port.PasswordHasher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BCryptPasswordHasher implements PasswordHasher {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    @Override
    public HashedPassword hash(RawPassword raw) {
        return new HashedPassword(encoder.encode(raw.value()));
    }

    @Override
    public boolean matches(RawPassword raw, HashedPassword hash) {
        return encoder.matches(raw.value(), hash.value());
    }
}
