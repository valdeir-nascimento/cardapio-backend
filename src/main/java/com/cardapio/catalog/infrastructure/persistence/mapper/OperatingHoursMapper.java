package com.cardapio.catalog.infrastructure.persistence.mapper;

import com.cardapio.catalog.domain.model.DayHours;
import com.cardapio.catalog.domain.model.OperatingHours;
import com.cardapio.catalog.domain.model.TimeRange;
import com.cardapio.catalog.infrastructure.persistence.jpa.OperatingHoursJpaEntity;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class OperatingHoursMapper {
    private OperatingHoursMapper() {}

    public static List<OperatingHoursJpaEntity> toJpaRows(OperatingHours hours) {
        List<OperatingHoursJpaEntity> rows = new ArrayList<>();
        for (Map.Entry<DayOfWeek, DayHours> entry : hours.snapshot().entrySet()) {
            short day = (short) entry.getKey().getValue();
            for (TimeRange range : entry.getValue().intervals()) {
                rows.add(new OperatingHoursJpaEntity(UUID.randomUUID(), day, range.openTime(), range.closeTime()));
            }
        }
        return rows;
    }

    public static OperatingHours toDomain(List<OperatingHoursJpaEntity> rows) {
        Map<DayOfWeek, List<TimeRange>> byDay = new EnumMap<>(DayOfWeek.class);
        for (OperatingHoursJpaEntity row : rows) {
            DayOfWeek day = DayOfWeek.of(row.getDayOfWeek());
            byDay.computeIfAbsent(day, k -> new ArrayList<>())
                 .add(TimeRange.of(row.getOpenTime(), row.getCloseTime()));
        }
        Map<DayOfWeek, DayHours> result = new EnumMap<>(DayOfWeek.class);
        byDay.forEach((d, ranges) -> result.put(d, new DayHours(d, ranges)));
        return OperatingHours.rehydrate(result);
    }
}
