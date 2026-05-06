package com.cardapio.notification.api.rest;

import com.cardapio.notification.infrastructure.sse.SseEmitterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
class AdminOrderStreamController {

    private final SseEmitterRegistry registry;

    @GetMapping(value = "/api/v1/admin/orders/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter stream() {
        SseEmitter emitter = registry.registerAdmin();
        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (Exception ignored) {
            // Client disconnected before initial send; cleanup hooks already remove the emitter.
        }
        return emitter;
    }
}
