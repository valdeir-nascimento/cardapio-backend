package com.cardapio.catalog.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimeRangeTest {

    @Test
    void acceptsValidRange() {
        TimeRange range = TimeRange.of(LocalTime.of(8, 0), LocalTime.of(18, 0));
        assertThat(range.openTime()).isEqualTo(LocalTime.of(8, 0));
        assertThat(range.closeTime()).isEqualTo(LocalTime.of(18, 0));
    }

    @Test
    void rejectsCloseBeforeOpen() {
        assertThatThrownBy(() -> TimeRange.of(LocalTime.of(18, 0), LocalTime.of(8, 0)))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("close");
    }

    @Test
    void rejectsEqualOpenAndClose() {
        assertThatThrownBy(() -> TimeRange.of(LocalTime.of(8, 0), LocalTime.of(8, 0)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void containsTimeWithinRange() {
        TimeRange range = TimeRange.of(LocalTime.of(8, 0), LocalTime.of(18, 0));
        assertThat(range.contains(LocalTime.of(12, 0))).isTrue();
        assertThat(range.contains(LocalTime.of(8, 0))).isTrue();   // inclusive open
        assertThat(range.contains(LocalTime.of(18, 0))).isFalse(); // exclusive close
        assertThat(range.contains(LocalTime.of(7, 59))).isFalse();
    }
}
