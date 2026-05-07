package com.cardapio.review.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RatingTest {

    @Test
    void acceptsOneToFive() {
        for (int stars = 1; stars <= 5; stars++) {
            assertThat(Rating.of(stars).stars()).isEqualTo(stars);
        }
    }

    @Test
    void rejectsZeroOrLess() {
        assertThatThrownBy(() -> Rating.of(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Rating.of(-1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsAboveFive() {
        assertThatThrownBy(() -> Rating.of(6)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Rating.of(100)).isInstanceOf(IllegalArgumentException.class);
    }
}
