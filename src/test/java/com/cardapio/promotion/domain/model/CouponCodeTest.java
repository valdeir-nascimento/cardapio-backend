package com.cardapio.promotion.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CouponCodeTest {

    @Test
    void normalizesToUppercaseAndTrims() {
        assertThat(CouponCode.of("  promo-1 ").value()).isEqualTo("PROMO-1");
        assertThat(CouponCode.of("welcome10").value()).isEqualTo("WELCOME10");
    }

    @Test
    void equalsByCanonicalForm() {
        assertThat(CouponCode.of("welcome10")).isEqualTo(CouponCode.of("WELCOME10"));
    }

    @Test
    void rejectsBlank() {
        assertThatThrownBy(() -> CouponCode.of("")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CouponCode.of("   ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsInvalidCharacters() {
        assertThatThrownBy(() -> CouponCode.of("PROMO!")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CouponCode.of("PROMO 1")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> CouponCode.of("PROMO_1")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsTooLong() {
        String tooLong = "A".repeat(33);
        assertThatThrownBy(() -> CouponCode.of(tooLong)).isInstanceOf(IllegalArgumentException.class);
    }
}
