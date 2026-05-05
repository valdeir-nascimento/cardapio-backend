package com.cardapio.catalog.domain.model;

import java.time.LocalTime;
import java.util.Objects;

public record TimeRange(LocalTime openTime, LocalTime closeTime) {

    public TimeRange {
        Objects.requireNonNull(openTime, "openTime");
        Objects.requireNonNull(closeTime, "closeTime");
        if (!closeTime.isAfter(openTime)) {
            throw new IllegalArgumentException("close time must be after open time");
        }
    }

    public static TimeRange of(LocalTime open, LocalTime close) { return new TimeRange(open, close); }

    public boolean contains(LocalTime time) {
        return !time.isBefore(openTime) && time.isBefore(closeTime);
    }
}
