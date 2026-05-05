package com.cardapio.catalog.application.usecase;

import com.cardapio.catalog.application.command.UpdateOperatingHoursCommand;
import com.cardapio.catalog.domain.model.OperatingHours;
import com.cardapio.catalog.domain.model.TimeRange;
import com.cardapio.catalog.domain.port.OperatingHoursRepository;
import com.cardapio.shared.domain.ErrorCode;
import com.cardapio.shared.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateOperatingHoursUseCase {

    private final OperatingHoursRepository repo;

    @Transactional
    public Result<Void> execute(UpdateOperatingHoursCommand cmd) {
        try {
            OperatingHours hours = repo.load().orElseGet(OperatingHours::empty);
            cmd.hoursByDay().forEach((day, ranges) -> hours.setHoursFor(day, ranges.stream()
                .map(r -> TimeRange.of(r.openTime(), r.closeTime()))
                .toList()));
            repo.save(hours);
            return Result.ok();
        } catch (IllegalArgumentException e) {
            return Result.failWith(ErrorCode.INVALID_HOURS, e.getMessage());
        }
    }
}
