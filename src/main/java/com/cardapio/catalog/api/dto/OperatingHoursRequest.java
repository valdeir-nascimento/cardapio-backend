package com.cardapio.catalog.api.dto;

import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

public record OperatingHoursRequest(@NotNull Map<DayOfWeek, List<TimeRangeDto>> hoursByDay) {
    public record TimeRangeDto(@NotNull LocalTime openTime, @NotNull LocalTime closeTime) {}
}
