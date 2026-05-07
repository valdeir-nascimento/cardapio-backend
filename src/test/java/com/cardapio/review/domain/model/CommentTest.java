package com.cardapio.review.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommentTest {

    @Test
    void normalizesNullToEmpty() {
        assertThat(Comment.of(null).value()).isEmpty();
        assertThat(Comment.of(null).isEmpty()).isTrue();
    }

    @Test
    void trimsWhitespace() {
        assertThat(Comment.of("   excellent   ").value()).isEqualTo("excellent");
    }

    @Test
    void blankBecomesEmpty() {
        assertThat(Comment.of("    ").isEmpty()).isTrue();
    }

    @Test
    void rejectsTooLong() {
        String tooLong = "a".repeat(501);
        assertThatThrownBy(() -> Comment.of(tooLong)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void acceptsAtBoundary() {
        String exactly500 = "a".repeat(500);
        assertThat(Comment.of(exactly500).value()).hasSize(500);
    }
}
