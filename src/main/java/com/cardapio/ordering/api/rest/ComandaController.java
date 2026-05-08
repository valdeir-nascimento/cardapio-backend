package com.cardapio.ordering.api.rest;

import com.cardapio.api.error.NotFoundException;
import com.cardapio.api.error.NotificationException;
import com.cardapio.identity.api.security.CardapioPrincipal;
import com.cardapio.ordering.api.dto.ComandaResponse;
import com.cardapio.ordering.api.dto.OpenComandaRequest;
import com.cardapio.ordering.application.command.JoinComandaCommand;
import com.cardapio.ordering.application.command.OpenComandaCommand;
import com.cardapio.ordering.application.usecase.JoinComandaUseCase;
import com.cardapio.ordering.application.usecase.OpenComandaUseCase;
import com.cardapio.ordering.domain.model.Comanda;
import com.cardapio.ordering.domain.model.ComandaId;
import com.cardapio.ordering.domain.model.TableId;
import com.cardapio.ordering.domain.port.ComandaRepository;
import com.cardapio.shared.domain.Notification;
import com.cardapio.shared.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/comandas")
@PreAuthorize("hasRole('CUSTOMER')")
public class ComandaController implements ComandaApi {

    private final OpenComandaUseCase openComanda;
    private final JoinComandaUseCase joinComanda;
    private final ComandaRepository comandas;

    @Override
    @PostMapping
    public ResponseEntity<ComandaResponse> open(
        @AuthenticationPrincipal CardapioPrincipal me,
        OpenComandaRequest body
    ) {
        var view = unwrap(openComanda.execute(new OpenComandaCommand(TableId.of(body.tableId()), me.subject())));
        return ResponseEntity.created(URI.create("/api/v1/comandas/" + view.id().value()))
            .body(ComandaResponse.from(view));
    }

    @Override
    @PostMapping("/{id}/join")
    public ComandaResponse join(
        @AuthenticationPrincipal CardapioPrincipal me,
        @PathVariable UUID id
    ) {
        var view = unwrap(joinComanda.execute(new JoinComandaCommand(ComandaId.of(id), me.subject())));
        return ComandaResponse.from(view);
    }

    @Override
    @GetMapping("/{id}")
    public ComandaResponse get(
        @AuthenticationPrincipal CardapioPrincipal me,
        @PathVariable UUID id
    ) {
        Comanda c = comandas.findById(ComandaId.of(id))
            .orElseThrow(() -> new NotFoundException(Notification.ofSingle(
                com.cardapio.shared.domain.ErrorCode.COMANDA_NOT_FOUND)));
        if (!c.hasMember(me.subject())) {
            throw new NotificationException(Notification.ofSingle(
                com.cardapio.shared.domain.ErrorCode.COMANDA_NOT_MEMBER));
        }
        return ComandaResponse.from(new com.cardapio.ordering.application.dto.ComandaView(
            c.id(), c.tableId(), c.status(), c.customerIds(), c.orderIds(), c.openedAt(), c.closedAt()));
    }

    private static <T> T unwrap(Result<T> r) {
        return r.orElseThrow(ComandaController::toException);
    }

    private static RuntimeException toException(Notification n) {
        boolean notFound = n.errors().stream()
            .anyMatch(e -> e.code() != null && e.code().endsWith("_NOT_FOUND"));
        return notFound ? new NotFoundException(n) : new NotificationException(n);
    }
}
