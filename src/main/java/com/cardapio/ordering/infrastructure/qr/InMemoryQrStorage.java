package com.cardapio.ordering.infrastructure.qr;

import com.cardapio.ordering.domain.port.QrStorage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fallback used in dev/test when R2 credentials are absent. Returns a
 * deterministic local URL that the front-end can render without a real bucket.
 */
@Component
@ConditionalOnProperty(prefix = "r2", name = "enabled", havingValue = "false", matchIfMissing = true)
public class InMemoryQrStorage implements QrStorage {

    private final Map<String, byte[]> store = new ConcurrentHashMap<>();

    @Override
    public void putIfAbsent(String key, byte[] bytes, String contentType) {
        store.putIfAbsent(key, bytes);
    }

    @Override
    public URI presignedUrl(String key, Duration ttl) {
        return URI.create("local://qr/" + key);
    }
}
