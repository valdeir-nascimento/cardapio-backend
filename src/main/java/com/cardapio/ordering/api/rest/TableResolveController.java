package com.cardapio.ordering.api.rest;

import com.cardapio.api.error.NotFoundException;
import com.cardapio.api.error.NotificationException;
import com.cardapio.ordering.api.dto.ResolveTableResponse;
import com.cardapio.ordering.application.usecase.ResolveTableTokenUseCase;
import com.cardapio.shared.domain.Notification;
import com.cardapio.shared.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/tables")
public class TableResolveController {

    private final ResolveTableTokenUseCase resolveTableToken;

    @GetMapping("/resolve")
    public ResolveTableResponse resolve(@RequestParam UUID token) {
        var view = unwrap(resolveTableToken.execute(token));
        return ResolveTableResponse.from(view);
    }

    private static <T> T unwrap(Result<T> r) {
        return r.orElseThrow(TableResolveController::toException);
    }

    private static RuntimeException toException(Notification n) {
        boolean notFound = n.errors().stream()
            .anyMatch(e -> e.code() != null && e.code().endsWith("_NOT_FOUND"));
        return notFound ? new NotFoundException(n) : new NotificationException(n);
    }
}
