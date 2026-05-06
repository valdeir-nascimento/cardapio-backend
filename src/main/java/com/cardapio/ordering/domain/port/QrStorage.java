package com.cardapio.ordering.domain.port;

import java.net.URI;
import java.time.Duration;

public interface QrStorage {
    void putIfAbsent(String key, byte[] bytes, String contentType);
    URI presignedUrl(String key, Duration ttl);
}
