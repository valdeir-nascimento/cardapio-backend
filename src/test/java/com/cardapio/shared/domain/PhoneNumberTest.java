package com.cardapio.shared.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhoneNumberTest {

    @Test
    void acceptsBrazilianMobileWithCountryCode() {
        PhoneNumber p = PhoneNumber.of("+55 11 91234-5678");
        assertThat(p.value()).isEqualTo("+5511912345678");
    }

    @Test
    void acceptsBrazilianLandline() {
        PhoneNumber p = PhoneNumber.of("+55 11 3123-4567");
        assertThat(p.value()).isEqualTo("+551131234567");
    }

    @Test
    void normalizesAddingPlusFiftyFiveWhenMissing() {
        PhoneNumber p = PhoneNumber.of("11912345678");
        assertThat(p.value()).isEqualTo("+5511912345678");
    }

    @Test
    void formattedReturnsHumanReadable() {
        PhoneNumber mobile = PhoneNumber.of("+5511912345678");
        assertThat(mobile.formatted()).isEqualTo("+55 (11) 91234-5678");

        PhoneNumber landline = PhoneNumber.of("+551131234567");
        assertThat(landline.formatted()).isEqualTo("+55 (11) 3123-4567");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "12345",                // too short
        "+551112345678901234",  // too long
        "abcdefghijk",          // letters
        ""
    })
    void rejectsInvalidPhone(String invalid) {
        assertThatThrownBy(() -> PhoneNumber.of(invalid))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
