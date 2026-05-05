// OperatingHoursView.java
package com.cardapio.catalog.application.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

public record OperatingHoursView(Map<DayOfWeek, List<TimeRangeView>> hoursByDay) {
    public record TimeRangeView(LocalTime openTime, LocalTime closeTime) {}
}
