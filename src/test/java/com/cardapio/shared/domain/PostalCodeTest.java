package com.cardapio.shared.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PostalCodeTest {

    @Test
    void acceptsUnformattedCep() {
        assertThat(PostalCode.of("01310100").value()).isEqualTo("01310100");
    }

    @Test
    void acceptsFormattedCep() {
        assertThat(PostalCode.of("01310-100").value()).isEqualTo("01310100");
    }

    @Test
    void formattedReturnsMaskedFormat() {
        assertThat(PostalCode.of("01310100").formatted()).isEqualTo("01310-100");
    }

    @ParameterizedTest
    @ValueSource(strings = {"1234567", "123456789", "abcdefgh", ""})
    void rejectsInvalidCep(String invalid) {
        assertThatThrownBy(() -> PostalCode.of(invalid))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
