package com.cardapio.shared.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CPFTest {

    // Valid CPF (test number with valid checksum): 529.982.247-25
    private static final String VALID = "52998224725";
    private static final String VALID_FORMATTED = "529.982.247-25";

    @Test
    void acceptsValidUnformattedCpf() {
        CPF cpf = CPF.of(VALID);
        assertThat(cpf.value()).isEqualTo(VALID);
    }

    @Test
    void acceptsValidFormattedCpf() {
        CPF cpf = CPF.of(VALID_FORMATTED);
        assertThat(cpf.value()).isEqualTo(VALID);
    }

    @Test
    void formattedReturnsMaskedFormat() {
        CPF cpf = CPF.of(VALID);
        assertThat(cpf.formatted()).isEqualTo(VALID_FORMATTED);
    }

    @Test
    void maskedReturnsLogSafeRepresentation() {
        CPF cpf = CPF.of(VALID);
        assertThat(cpf.masked()).isEqualTo("***.***.***-25");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "11111111111",        // all same digits — rejected
        "12345678900",        // invalid checksum
        "1234567890",         // 10 digits
        "123456789012",       // 12 digits
        "abcdefghijk",        // letters
        ""                    // empty
    })
    void rejectsInvalidCpf(String invalid) {
        assertThatThrownBy(() -> CPF.of(invalid))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("invalid CPF");
    }

    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> CPF.of(null))
            .isInstanceOf(NullPointerException.class);
    }
}
