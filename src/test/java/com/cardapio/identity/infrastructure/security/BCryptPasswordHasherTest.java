package com.cardapio.identity.infrastructure.security;

import com.cardapio.identity.domain.model.HashedPassword;
import com.cardapio.identity.domain.model.RawPassword;
import com.cardapio.identity.domain.port.PasswordHasher;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BCryptPasswordHasherTest {

    private final PasswordHasher hasher = new BCryptPasswordHasher();

    @Test
    void hashAndMatch() {
        RawPassword raw = RawPassword.of("S3cret!Password");
        HashedPassword hash = hasher.hash(raw);

        assertThat(hash.value()).startsWith("$2");
        assertThat(hasher.matches(raw, hash)).isTrue();
        assertThat(hasher.matches(RawPassword.of("Wrong!Pass1"), hash)).isFalse();
    }

    @Test
    void differentHashesEachTime() {
        RawPassword raw = RawPassword.of("S3cret!Password");
        HashedPassword h1 = hasher.hash(raw);
        HashedPassword h2 = hasher.hash(raw);
        assertThat(h1.value()).isNotEqualTo(h2.value());
        assertThat(hasher.matches(raw, h1)).isTrue();
        assertThat(hasher.matches(raw, h2)).isTrue();
    }
}
