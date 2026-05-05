package com.cardapio.catalog.domain.model;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OperatingHoursTest {

    @Test
    void emptyOperatingHoursIsClosedAlways() {
        OperatingHours hours = OperatingHours.empty();
        assertThat(hours.isOpenAt(LocalDateTime.of(2026, 5, 4, 12, 0))).isFalse();
    }

    @Test
    void setHoursForSingleDay() {
        OperatingHours hours = OperatingHours.empty();
        hours.setHoursFor(DayOfWeek.MONDAY,
            List.of(TimeRange.of(LocalTime.of(11, 0), LocalTime.of(15, 0)),
                    TimeRange.of(LocalTime.of(18, 0), LocalTime.of(23, 0))));

        assertThat(hours.isOpenAt(LocalDateTime.of(2026, 5, 4, 12, 0))).isTrue();   // Monday lunch
        assertThat(hours.isOpenAt(LocalDateTime.of(2026, 5, 4, 16, 0))).isFalse();  // Monday gap
        assertThat(hours.isOpenAt(LocalDateTime.of(2026, 5, 4, 22, 0))).isTrue();   // Monday dinner
        assertThat(hours.isOpenAt(LocalDateTime.of(2026, 5, 5, 12, 0))).isFalse();  // Tuesday — not configured
    }

    @Test
    void replacingDayHoursOverrides() {
        OperatingHours hours = OperatingHours.empty();
        hours.setHoursFor(DayOfWeek.MONDAY, List.of(TimeRange.of(LocalTime.of(8, 0), LocalTime.of(10, 0))));
        hours.setHoursFor(DayOfWeek.MONDAY, List.of(TimeRange.of(LocalTime.of(11, 0), LocalTime.of(15, 0))));

        assertThat(hours.isOpenAt(LocalDateTime.of(2026, 5, 4, 9, 0))).isFalse();
        assertThat(hours.isOpenAt(LocalDateTime.of(2026, 5, 4, 12, 0))).isTrue();
    }

    @Test
    void exposesHoursForAllDays() {
        OperatingHours hours = OperatingHours.empty();
        hours.setHoursFor(DayOfWeek.WEDNESDAY,
            List.of(TimeRange.of(LocalTime.of(9, 0), LocalTime.of(17, 0))));

        DayHours wed = hours.hoursFor(DayOfWeek.WEDNESDAY);
        assertThat(wed.intervals()).hasSize(1);
    }
}
