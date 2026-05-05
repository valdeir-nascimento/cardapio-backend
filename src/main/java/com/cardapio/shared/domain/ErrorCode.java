package com.cardapio.shared.domain;

public enum ErrorCode {

    // Generic / shared
    INVALID_INPUT("entrada inválida"),

    // Identity
    INVALID_CREDENTIALS("credenciais inválidas"),
    INVALID_REFRESH_TOKEN("refresh token inválido"),
    EMAIL_ALREADY_REGISTERED("este e-mail já está cadastrado"),
    INVALID_EMAIL("e-mail inválido"),
    INVALID_PHONE("telefone inválido"),
    WEAK_PASSWORD("senha fraca"),
    BLANK_NAME("nome obrigatório"),
    CUSTOMER_NOT_FOUND("cliente não encontrado"),

    // Catalog – Category
    CATEGORY_NOT_FOUND("categoria não encontrada"),
    CATEGORY_HAS_PRODUCTS("categoria tem produtos vinculados"),
    INVALID_DISPLAY_ORDER("ordem inválida"),

    // Catalog – Product
    PRODUCT_NOT_FOUND("produto não encontrado"),
    INVALID_PRICE("preço inválido"),
    INVALID_STOCK("estoque inválido"),

    // Catalog – Operating Hours
    INVALID_HOURS("horário inválido");

    private final String defaultMessage;

    ErrorCode(String defaultMessage) {
        this.defaultMessage = defaultMessage;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
