package com.cardapio.catalog.domain.model;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class DayHours {

    private final DayOfWeek dayOfWeek;
    private final List<TimeRange> intervals;

    public DayHours(DayOfWeek dayOfWeek, List<TimeRange> intervals) {
        this.dayOfWeek = Objects.requireNonNull(dayOfWeek, "dayOfWeek");
        this.intervals = List.copyOf(Objects.requireNonNull(intervals, "intervals"));
    }

    public boolean contains(LocalTime time) {
        return intervals.stream().anyMatch(r -> r.contains(time));
    }

    public DayOfWeek dayOfWeek() { return dayOfWeek; }
    public List<TimeRange> intervals() { return Collections.unmodifiableList(intervals); }
}
