package com.cardapio.promotion.domain.model;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

public record CouponCode(String value) {

    private static final Pattern ALLOWED = Pattern.compile("^[A-Z0-9-]{1,32}$");

    public CouponCode {
        Objects.requireNonNull(value, "value");
        value = value.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED.matcher(value).matches()) {
            throw new IllegalArgumentException("invalid coupon code: " + value);
        }
    }

    public static CouponCode of(String raw) {
        return new CouponCode(raw);
    }
}
