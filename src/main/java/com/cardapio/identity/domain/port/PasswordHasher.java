package com.cardapio.identity.domain.port;

import com.cardapio.identity.domain.model.HashedPassword;
import com.cardapio.identity.domain.model.RawPassword;

public interface PasswordHasher {
    HashedPassword hash(RawPassword raw);
    boolean matches(RawPassword raw, HashedPassword hash);
}
