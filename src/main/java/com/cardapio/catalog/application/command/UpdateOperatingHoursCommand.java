// UpdateOperatingHoursCommand.java
package com.cardapio.catalog.application.command;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

public record UpdateOperatingHoursCommand(Map<DayOfWeek, List<TimeRangeDraft>> hoursByDay) {
    public record TimeRangeDraft(LocalTime openTime, LocalTime closeTime) {}
}
