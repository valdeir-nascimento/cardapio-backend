package com.cardapio.notification.infrastructure.sse;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SseEmitterRegistryTest {

    private final SseEmitterRegistry registry = new SseEmitterRegistry();

    @Test
    void adminEmitterIsTracked() {
        SseEmitter e = registry.registerAdmin();
        assertThat(e).isNotNull();
        assertThat(registry.adminCount()).isEqualTo(1);
    }

    @Test
    void customerEmitterIsTrackedPerId() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        registry.registerCustomer(a);
        registry.registerCustomer(a);
        registry.registerCustomer(b);
        assertThat(registry.customerCount(a)).isEqualTo(2);
        assertThat(registry.customerCount(b)).isEqualTo(1);
        assertThat(registry.customerCount(UUID.randomUUID())).isZero();
    }

    @Test
    void sendToUnknownCustomerIsNoOp() {
        registry.sendToCustomer(UUID.randomUUID(), "evt", "data");
        // no exception
    }

    @Test
    void sendDropsBrokenEmitters() {
        UUID id = UUID.randomUUID();
        SseEmitter e = registry.registerCustomer(id);
        e.completeWithError(new RuntimeException("client gone"));

        registry.sendToCustomer(id, "evt", "data");
        assertThat(registry.customerCount(id)).isZero();
    }
}
