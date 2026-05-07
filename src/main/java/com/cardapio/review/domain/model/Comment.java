package com.cardapio.review.domain.model;

public record Comment(String value) {

    private static final int MAX_LENGTH = 500;

    public Comment {
        value = value == null ? "" : value.trim();
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("comment exceeds " + MAX_LENGTH + " characters");
        }
    }

    public static Comment empty() {
        return new Comment("");
    }

    public static Comment of(String raw) {
        return new Comment(raw);
    }

    public boolean isEmpty() {
        return value.isEmpty();
    }
}
