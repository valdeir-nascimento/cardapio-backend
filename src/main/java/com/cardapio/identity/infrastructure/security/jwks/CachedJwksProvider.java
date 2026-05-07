package com.cardapio.identity.infrastructure.security.jwks;

import lombok.extern.slf4j.Slf4j;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Caches a remote JWK set per URL with a TTL. On cache miss for a kid the
 * provider performs a one-shot refetch before failing — providers rotate
 * keys without notice.
 */
@Slf4j
public class CachedJwksProvider {

    private final JwksClient client;
    private final Duration ttl;
    private final Clock clock;
    private final Map<String, CacheEntry> cache = new HashMap<>();
    private final ReentrantLock lock = new ReentrantLock();

    public CachedJwksProvider(JwksClient client, Duration ttl, Clock clock) {
        this.client = client;
        this.ttl = ttl;
        this.clock = clock;
    }

    public PublicKey getPublicKey(String url, String kid) {
        PublicKey cached = lookup(url, kid, false);
        if (cached != null) return cached;
        // kid miss — refetch once
        PublicKey refreshed = lookup(url, kid, true);
        if (refreshed == null) {
            throw new IllegalStateException("no key with kid='" + kid + "' at " + url);
        }
        return refreshed;
    }

    private PublicKey lookup(String url, String kid, boolean forceRefresh) {
        lock.lock();
        try {
            CacheEntry entry = cache.get(url);
            if (forceRefresh || entry == null || entry.expiresAt.isBefore(clock.instant())) {
                JwksClient.JwkSet set = client.fetch(url);
                Map<String, PublicKey> keys = new HashMap<>();
                for (JwksClient.Jwk jwk : set.keys()) {
                    PublicKey key = parseJwk(jwk);
                    if (key != null) keys.put(jwk.kid(), key);
                }
                entry = new CacheEntry(keys, clock.instant().plus(ttl));
                cache.put(url, entry);
            }
            return entry.keys.get(kid);
        } finally {
            lock.unlock();
        }
    }

    private PublicKey parseJwk(JwksClient.Jwk jwk) {
        try {
            return switch (jwk.kty()) {
                case "RSA" -> rsaKey(jwk);
                case "EC"  -> ecKey(jwk);
                default -> {
                    log.warn("Unsupported JWK kty='{}' kid='{}' — skipping", jwk.kty(), jwk.kid());
                    yield null;
                }
            };
        } catch (Exception e) {
            log.warn("Failed to parse JWK kid='{}': {}", jwk.kid(), e.getMessage());
            return null;
        }
    }

    private static PublicKey rsaKey(JwksClient.Jwk jwk) throws Exception {
        BigInteger n = new BigInteger(1, Base64.getUrlDecoder().decode(jwk.modulus()));
        BigInteger e = new BigInteger(1, Base64.getUrlDecoder().decode(jwk.exponent()));
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(n, e));
    }

    private static PublicKey ecKey(JwksClient.Jwk jwk) throws Exception {
        BigInteger x = new BigInteger(1, Base64.getUrlDecoder().decode(jwk.x()));
        BigInteger y = new BigInteger(1, Base64.getUrlDecoder().decode(jwk.y()));
        ECParameterSpec params = EcCurves.params(jwk.curve());
        ECPoint point = new ECPoint(x, y);
        return (ECPublicKey) KeyFactory.getInstance("EC").generatePublic(new ECPublicKeySpec(point, params));
    }

    private record CacheEntry(Map<String, PublicKey> keys, Instant expiresAt) {}
}
