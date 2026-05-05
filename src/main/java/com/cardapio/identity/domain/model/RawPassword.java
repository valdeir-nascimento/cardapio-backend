package com.cardapio.identity.domain.model;

import java.util.Objects;
import java.util.regex.Pattern;

public final class RawPassword {

    private static final Pattern HAS_LOWER = Pattern.compile(".*[a-z].*");
    private static final Pattern HAS_UPPER = Pattern.compile(".*[A-Z].*");
    private static final Pattern HAS_DIGIT = Pattern.compile(".*\\d.*");
    private static final Pattern HAS_SYMBOL = Pattern.compile(".*[^A-Za-z0-9].*");

    private final String value;

    private RawPassword(String value) {
        Objects.requireNonNull(value, "value");
        if (value.length() < 8
            || !HAS_LOWER.matcher(value).matches()
            || !HAS_UPPER.matcher(value).matches()
            || !HAS_DIGIT.matcher(value).matches()
            || !HAS_SYMBOL.matcher(value).matches()) {
            throw new IllegalArgumentException(
                "weak password: must be ≥8 chars with upper, lower, digit and symbol");
        }
        this.value = value;
    }

    public static RawPassword of(String raw) { return new RawPassword(raw); }

    public String value() { return value; }

    @Override public String toString() { return "RawPassword[***]"; }
}
