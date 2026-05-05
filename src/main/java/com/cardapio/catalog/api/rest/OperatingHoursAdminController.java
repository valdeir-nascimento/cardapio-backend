package com.cardapio.catalog.api.rest;

import com.cardapio.catalog.api.dto.OperatingHoursRequest;
import com.cardapio.catalog.application.CatalogFacade;
import com.cardapio.catalog.application.command.UpdateOperatingHoursCommand;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class OperatingHoursAdminController {

    private final CatalogFacade catalog;

    @PutMapping
    public ResponseEntity<Void> updateAll(@Valid @RequestBody OperatingHoursRequest req) {
        Map<DayOfWeek, List<UpdateOperatingHoursCommand.TimeRangeDraft>> map = req.hoursByDay().entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().stream()
                .map(t -> new UpdateOperatingHoursCommand.TimeRangeDraft(t.openTime(), t.closeTime()))
                .toList()));
        catalog.updateOperatingHours(new UpdateOperatingHoursCommand(map));
        return ResponseEntity.noContent().build();
    }
}
