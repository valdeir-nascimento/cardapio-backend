package com.cardapio.notification.api.rest;

import com.cardapio.api.error.NotFoundException;
import com.cardapio.identity.api.security.CardapioPrincipal;
import com.cardapio.notification.domain.port.OrderSummaryPort;
import com.cardapio.notification.infrastructure.sse.SseEmitterRegistry;
import com.cardapio.shared.domain.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
class CustomerOrderStreamController {

    private final SseEmitterRegistry registry;
    private final OrderSummaryPort orderSummary;

    @GetMapping(value = "/api/v1/orders/{id}/status/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter stream(@AuthenticationPrincipal CardapioPrincipal me, @PathVariable UUID id) {
        var summary = orderSummary.find(id)
            .orElseThrow(() -> new NotFoundException(Notification.empty()));
        if (!summary.customerId().equals(me.subject())) {
            throw new AccessDeniedException("order does not belong to caller");
        }

        SseEmitter emitter = registry.registerCustomer(me.subject());
        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (Exception ignored) {
            // Client disconnected before initial send.
        }
        return emitter;
    }
}
