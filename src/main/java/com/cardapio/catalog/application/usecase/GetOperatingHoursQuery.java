// GetOperatingHoursQuery.java
package com.cardapio.catalog.application.usecase;

import com.cardapio.catalog.application.dto.OperatingHoursView;
import com.cardapio.catalog.domain.model.OperatingHours;
import com.cardapio.catalog.domain.port.OperatingHoursRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class GetOperatingHoursQuery {
    private final OperatingHoursRepository repo;
    public GetOperatingHoursQuery(OperatingHoursRepository repo) { this.repo = repo; }

    @Transactional(readOnly = true)
    public OperatingHoursView execute() {
        OperatingHours hours = repo.load().orElseGet(OperatingHours::empty);
        Map<DayOfWeek, List<OperatingHoursView.TimeRangeView>> result = new EnumMap<>(DayOfWeek.class);
        hours.snapshot().forEach((day, dh) -> {
            result.put(day, dh.intervals().stream()
                .map(r -> new OperatingHoursView.TimeRangeView(r.openTime(), r.closeTime()))
                .toList());
        });
        return new OperatingHoursView(result);
    }
}
