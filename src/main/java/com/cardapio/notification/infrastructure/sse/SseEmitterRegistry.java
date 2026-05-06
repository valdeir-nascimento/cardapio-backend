package com.cardapio.notification.infrastructure.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@Slf4j
public class SseEmitterRegistry {

    private final List<SseEmitter> adminEmitters = new CopyOnWriteArrayList<>();
    private final Map<UUID, List<SseEmitter>> customerEmitters = new ConcurrentHashMap<>();

    public SseEmitter registerAdmin() {
        SseEmitter emitter = newEmitter();
        adminEmitters.add(emitter);
        emitter.onCompletion(() -> adminEmitters.remove(emitter));
        emitter.onTimeout(() -> { adminEmitters.remove(emitter); emitter.complete(); });
        emitter.onError(t -> adminEmitters.remove(emitter));
        return emitter;
    }

    public SseEmitter registerCustomer(UUID customerId) {
        SseEmitter emitter = newEmitter();
        List<SseEmitter> list = customerEmitters.computeIfAbsent(customerId,
            k -> new CopyOnWriteArrayList<>());
        list.add(emitter);
        emitter.onCompletion(() -> list.remove(emitter));
        emitter.onTimeout(() -> { list.remove(emitter); emitter.complete(); });
        emitter.onError(t -> list.remove(emitter));
        return emitter;
    }

    public void sendToAdmins(String name, Object payload) {
        send(adminEmitters, name, payload);
    }

    public void sendToCustomer(UUID customerId, String name, Object payload) {
        List<SseEmitter> list = customerEmitters.get(customerId);
        if (list != null) send(list, name, payload);
    }

    private static void send(List<SseEmitter> emitters, String name, Object payload) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(name).data(payload));
            } catch (IOException | IllegalStateException e) {
                emitters.remove(emitter);
            }
        }
    }

    /** Heartbeat keeps proxies (Cloudflare/Nginx default 60s idle) from dropping the stream. */
    @Scheduled(fixedDelay = 25_000L)
    void heartbeat() {
        sendToAdmins("ping", "");
        for (Map.Entry<UUID, List<SseEmitter>> e : customerEmitters.entrySet()) {
            send(e.getValue(), "ping", "");
        }
    }

    private static SseEmitter newEmitter() {
        return new SseEmitter(0L); // never times out — rely on client disconnect / heartbeat failure
    }

    int adminCount() { return adminEmitters.size(); }
    int customerCount(UUID id) {
        List<SseEmitter> l = customerEmitters.get(id);
        return l == null ? 0 : l.size();
    }
}
