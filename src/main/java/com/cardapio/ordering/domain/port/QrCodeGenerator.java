package com.cardapio.ordering.domain.port;

public interface QrCodeGenerator {
    byte[] generatePng(String token, int sizePx);
}
