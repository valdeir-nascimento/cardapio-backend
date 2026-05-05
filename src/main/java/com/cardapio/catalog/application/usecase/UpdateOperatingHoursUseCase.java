// UpdateOperatingHoursUseCase.java
package com.cardapio.catalog.application.usecase;

import com.cardapio.catalog.application.command.UpdateOperatingHoursCommand;
import com.cardapio.catalog.domain.model.OperatingHours;
import com.cardapio.catalog.domain.model.TimeRange;
import com.cardapio.catalog.domain.port.OperatingHoursRepository;
import com.cardapio.shared.domain.Notification;
import com.cardapio.shared.domain.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateOperatingHoursUseCase {
    private final OperatingHoursRepository repo;
    public UpdateOperatingHoursUseCase(OperatingHoursRepository repo) { this.repo = repo; }

    @Transactional
    public Result<Void> execute(UpdateOperatingHoursCommand cmd) {
        try {
            OperatingHours hours = repo.load().orElseGet(OperatingHours::empty);
            cmd.hoursByDay().forEach((day, ranges) -> {
                hours.setHoursFor(day, ranges.stream()
                    .map(r -> TimeRange.of(r.openTime(), r.closeTime()))
                    .toList());
            });
            repo.save(hours);
            return Result.success(null);
        } catch (IllegalArgumentException e) {
            Notification n = Notification.empty();
            n.addError("INVALID_HOURS", e.getMessage());
            return Result.failure(n);
        }
    }
}
