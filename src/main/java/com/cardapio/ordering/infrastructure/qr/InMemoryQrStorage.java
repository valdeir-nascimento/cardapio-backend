package com.cardapio.ordering.infrastructure.qr;

import com.cardapio.ordering.domain.port.QrStorage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Fallback used in dev/test when R2 credentials are absent. Returns a
 * deterministic local URL that the front-end can render without a real bucket.
 */
@Component
@ConditionalOnMissingBean(S3Client.class)
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
