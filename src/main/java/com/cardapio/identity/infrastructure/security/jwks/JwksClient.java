package com.cardapio.identity.infrastructure.security.jwks;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Thin RestClient wrapper that fetches a JWK Set from a URL. The provider
 * may rotate keys, so this client is purposely stateless — the cache layer
 * lives in CachedJwksProvider.
 */
public class JwksClient {

    private final RestClient http;

    public JwksClient(RestClient http) {
        this.http = http;
    }

    public JwkSet fetch(String url) {
        JwkSet result = http.get().uri(url).retrieve().body(JwkSet.class);
        if (result == null || result.keys() == null) {
            throw new IllegalStateException("empty JWK set from " + url);
        }
        return result;
    }

    public record JwkSet(@JsonProperty("keys") List<Jwk> keys) {}

    public record Jwk(
        @JsonProperty("kid") String kid,
        @JsonProperty("kty") String kty,
        @JsonProperty("alg") String alg,
        @JsonProperty("use") String use,
        @JsonProperty("n")   String modulus,
        @JsonProperty("e")   String exponent,
        @JsonProperty("crv") String curve,
        @JsonProperty("x")   String x,
        @JsonProperty("y")   String y
    ) {}
}
