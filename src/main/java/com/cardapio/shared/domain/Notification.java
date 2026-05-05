package com.cardapio.shared.domain;

import java.util.ArrayList;
import java.util.List;

public final class Notification {

    private final List<NotificationError> errors = new ArrayList<>();

    private Notification() {}

    public static Notification empty() {
        return new Notification();
    }

    public void addError(String code, String message) {
        errors.add(new NotificationError(null, code, message));
    }

    public void addError(String field, String code, String message) {
        errors.add(new NotificationError(field, code, message));
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public List<NotificationError> errors() {
        return List.copyOf(errors);
    }

    public void merge(Notification other) {
        errors.addAll(other.errors);
    }
}
