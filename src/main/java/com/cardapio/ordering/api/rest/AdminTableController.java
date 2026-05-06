package com.cardapio.ordering.api.rest;

import com.cardapio.api.error.NotFoundException;
import com.cardapio.api.error.NotificationException;
import com.cardapio.ordering.api.dto.CreateTableRequest;
import com.cardapio.ordering.api.dto.TableQrResponse;
import com.cardapio.ordering.api.dto.TableResponse;
import com.cardapio.ordering.application.command.CreateTableCommand;
import com.cardapio.ordering.application.usecase.CreateTableUseCase;
import com.cardapio.ordering.application.usecase.GetTableQrUseCase;
import com.cardapio.ordering.application.usecase.ListTablesUseCase;
import com.cardapio.ordering.domain.model.TableId;
import com.cardapio.shared.domain.Notification;
import com.cardapio.shared.domain.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/tables")
@PreAuthorize("hasAnyRole('OWNER','MANAGER','OPERATOR')")
public class AdminTableController {

    private final CreateTableUseCase createTable;
    private final ListTablesUseCase listTables;
    private final GetTableQrUseCase getTableQr;

    @PostMapping
    public ResponseEntity<TableResponse> create(@Valid @RequestBody CreateTableRequest body) {
        var view = unwrap(createTable.execute(new CreateTableCommand(body.number())));
        return ResponseEntity
            .created(URI.create("/api/v1/admin/tables/" + view.id().value()))
            .body(TableResponse.from(view));
    }

    @GetMapping
    public List<TableResponse> list() {
        return listTables.execute().stream().map(TableResponse::from).toList();
    }

    @GetMapping("/{id}/qr")
    public TableQrResponse qr(@PathVariable UUID id) {
        var view = unwrap(getTableQr.execute(TableId.of(id)));
        return TableQrResponse.from(view);
    }

    private static <T> T unwrap(Result<T> r) {
        return r.orElseThrow(AdminTableController::toException);
    }

    private static RuntimeException toException(Notification n) {
        boolean notFound = n.errors().stream()
            .anyMatch(e -> e.code() != null && e.code().endsWith("_NOT_FOUND"));
        return notFound ? new NotFoundException(n) : new NotificationException(n);
    }
}
