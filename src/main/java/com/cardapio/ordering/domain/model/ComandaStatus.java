package com.cardapio.ordering.domain.model;

public enum ComandaStatus {
    OPEN, CLOSED;

    public boolean isTerminal() {
        return this == CLOSED;
    }
}
