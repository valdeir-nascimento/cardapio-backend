package com.cardapio.ordering.api.rest;

import com.cardapio.api.error.NotFoundException;
import com.cardapio.api.error.NotificationException;
import com.cardapio.ordering.api.dto.ComandaResponse;
import com.cardapio.ordering.application.command.CloseComandaCommand;
import com.cardapio.ordering.application.dto.ComandaSummaryView;
import com.cardapio.ordering.application.usecase.CloseComandaUseCase;
import com.cardapio.ordering.domain.model.Comanda;
import com.cardapio.ordering.domain.model.ComandaId;
import com.cardapio.ordering.domain.model.ComandaStatus;
import com.cardapio.ordering.domain.port.ComandaRepository;
import com.cardapio.shared.domain.Notification;
import com.cardapio.shared.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/comandas")
@PreAuthorize("hasAnyRole('OWNER','MANAGER','OPERATOR')")
public class AdminComandaController {

    private final ComandaRepository comandas;
    private final CloseComandaUseCase closeComanda;

    @GetMapping
    public List<ComandaResponse> list(@RequestParam(defaultValue = "OPEN") ComandaStatus status) {
        return comandas.findByStatus(status).stream()
            .map(this::toSummaryResponse)
            .toList();
    }

    @PostMapping("/{id}/close")
    public ComandaResponse close(@PathVariable UUID id) {
        var view = unwrap(closeComanda.execute(new CloseComandaCommand(ComandaId.of(id))));
        return ComandaResponse.from(view);
    }

    private ComandaResponse toSummaryResponse(Comanda c) {
        return ComandaResponse.from(new ComandaSummaryView(
            c.id(), c.tableId(), c.status(),
            c.customerIds().size(), c.orderIds().size(),
            c.openedAt(), c.closedAt()));
    }

    private static <T> T unwrap(Result<T> r) {
        return r.orElseThrow(AdminComandaController::toException);
    }

    private static RuntimeException toException(Notification n) {
        boolean notFound = n.errors().stream()
            .anyMatch(e -> e.code() != null && e.code().endsWith("_NOT_FOUND"));
        return notFound ? new NotFoundException(n) : new NotificationException(n);
    }
}
