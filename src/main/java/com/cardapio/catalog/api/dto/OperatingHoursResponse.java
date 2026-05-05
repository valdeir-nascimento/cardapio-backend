package com.cardapio.catalog.api.dto;

import com.cardapio.catalog.application.dto.OperatingHoursView;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record OperatingHoursResponse(Map<DayOfWeek, List<TimeRangeDto>> hoursByDay) {
    public record TimeRangeDto(LocalTime openTime, LocalTime closeTime) {}

    public static OperatingHoursResponse from(OperatingHoursView v) {
        return new OperatingHoursResponse(v.hoursByDay().entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey,
                e -> e.getValue().stream().map(t -> new TimeRangeDto(t.openTime(), t.closeTime())).toList())));
    }
}
