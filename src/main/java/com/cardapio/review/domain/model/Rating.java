package com.cardapio.review.domain.model;

public record Rating(int stars) {
    public Rating {
        if (stars < 1 || stars > 5) {
            throw new IllegalArgumentException("rating must be between 1 and 5");
        }
    }

    public static Rating of(int stars) {
        return new Rating(stars);
    }
}
