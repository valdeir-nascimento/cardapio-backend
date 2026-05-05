package com.cardapio.shared.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailTest {

    @Test
    void acceptsValidEmail() {
        Email email = Email.of("user@example.com");
        assertThat(email.value()).isEqualTo("user@example.com");
    }

    @Test
    void normalizesToLowerCaseAndTrim() {
        Email email = Email.of("  USER@Example.COM  ");
        assertThat(email.value()).isEqualTo("user@example.com");
    }

    @ParameterizedTest
    @ValueSource(strings = {"plainaddress", "@no-local.com", "no-at.com", "double@@x.com", "spaces in@x.com", ""})
    void rejectsInvalidEmail(String invalid) {
        assertThatThrownBy(() -> Email.of(invalid))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("invalid email");
    }

    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> Email.of(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void equalsAndHashCodeAreValueBased() {
        assertThat(Email.of("a@b.com")).isEqualTo(Email.of("A@B.COM"));
    }
}
