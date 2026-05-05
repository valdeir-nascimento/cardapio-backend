package com.cardapio.identity.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RawPasswordTest {

    @Test
    void acceptsStrongPassword() {
        RawPassword p = RawPassword.of("S3curePass!");
        assertThat(p.value()).isEqualTo("S3curePass!");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "short1!",         // < 8 chars
        "alllowercase1!",  // no uppercase
        "ALLUPPERCASE1!",  // no lowercase
        "NoNumbers!",      // no digit
        "NoSymbols123",    // no symbol
        ""                 // empty
    })
    void rejectsWeakPassword(String weak) {
        assertThatThrownBy(() -> RawPassword.of(weak))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("password");
    }

    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> RawPassword.of(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void toStringDoesNotExposeValue() {
        RawPassword p = RawPassword.of("S3curePass!");
        assertThat(p.toString()).doesNotContain("S3curePass!");
    }
}
