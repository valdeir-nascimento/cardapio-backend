package com.cardapio.catalog.domain.model;

import com.cardapio.shared.domain.AggregateRoot;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class OperatingHours extends AggregateRoot<UUID> {

    public static final UUID SINGLETON_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final Map<DayOfWeek, DayHours> hoursByDay;

    private OperatingHours(UUID id, Map<DayOfWeek, DayHours> hoursByDay) {
        super(id);
        this.hoursByDay = new EnumMap<>(hoursByDay);
    }

    public static OperatingHours empty() {
        return new OperatingHours(SINGLETON_ID, new EnumMap<>(DayOfWeek.class));
    }

    public static OperatingHours rehydrate(Map<DayOfWeek, DayHours> hoursByDay) {
        return new OperatingHours(SINGLETON_ID, hoursByDay);
    }

    public void setHoursFor(DayOfWeek day, List<TimeRange> intervals) {
        if (intervals == null || intervals.isEmpty()) {
            hoursByDay.remove(day);
        } else {
            hoursByDay.put(day, new DayHours(day, intervals));
        }
    }

    public DayHours hoursFor(DayOfWeek day) {
        return hoursByDay.getOrDefault(day, new DayHours(day, List.of()));
    }

    public boolean isOpenAt(LocalDateTime when) {
        DayHours dh = hoursByDay.get(when.getDayOfWeek());
        return dh != null && dh.contains(when.toLocalTime());
    }

    public Map<DayOfWeek, DayHours> snapshot() { return Map.copyOf(hoursByDay); }
}
