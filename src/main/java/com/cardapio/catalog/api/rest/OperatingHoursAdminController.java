package com.cardapio.catalog.api.rest;

import com.cardapio.api.error.ProblemDetails;
import com.cardapio.catalog.api.dto.OperatingHoursRequest;
import com.cardapio.catalog.application.command.UpdateOperatingHoursCommand;
import com.cardapio.catalog.application.usecase.UpdateOperatingHoursUseCase;
import com.cardapio.shared.domain.Result;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/operating-hours")
@PreAuthorize("hasAnyRole('OWNER','MANAGER')")
public class OperatingHoursAdminController {

    private final UpdateOperatingHoursUseCase update;

    public OperatingHoursAdminController(UpdateOperatingHoursUseCase update) { this.update = update; }

    @PutMapping
    public ResponseEntity<?> updateAll(@Valid @RequestBody OperatingHoursRequest req) {
        Map<DayOfWeek, List<UpdateOperatingHoursCommand.TimeRangeDraft>> map = req.hoursByDay().entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().stream()
                .map(t -> new UpdateOperatingHoursCommand.TimeRangeDraft(t.openTime(), t.closeTime()))
                .toList()));
        Result<Void> r = update.execute(new UpdateOperatingHoursCommand(map));
        return switch (r) {
            case Result.Success<Void> s -> ResponseEntity.noContent().build();
            case Result.Failure<Void> f -> ResponseEntity.unprocessableEntity()
                .contentType(MediaType.parseMediaType("application/problem+json"))
                .body((ProblemDetail) ProblemDetails.fromNotification(f.notification()));
        };
    }
}
