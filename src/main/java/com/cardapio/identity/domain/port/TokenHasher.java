package com.cardapio.identity.domain.port;

public interface TokenHasher {
    String sha256Hex(String input);
}
