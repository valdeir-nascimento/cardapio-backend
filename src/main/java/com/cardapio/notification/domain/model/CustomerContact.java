package com.cardapio.notification.domain.model;

import java.util.Objects;
import java.util.UUID;

public record CustomerContact(UUID id, String name, String email, String phoneNumber) {
    public CustomerContact {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
    }
}
