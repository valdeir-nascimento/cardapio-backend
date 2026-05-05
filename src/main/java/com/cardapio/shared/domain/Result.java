package com.cardapio.shared.domain;

import java.util.function.Function;

public sealed interface Result<T> permits Result.Success, Result.Failure {

    record Success<T>(T value) implements Result<T> {}
    record Failure<T>(Notification notification) implements Result<T> {}

    static <T> Result<T> success(T value) {
        return new Success<>(value);
    }

    static <T> Result<T> failure(Notification notification) {
        return new Failure<>(notification);
    }

    default boolean isSuccess() {
        return this instanceof Success<T>;
    }

    default T getOrThrow() {
        return switch (this) {
            case Success<T> s -> s.value();
            case Failure<T> f -> throw new IllegalStateException(
                "Result is a Failure: " + f.notification().errors());
        };
    }

    default <R> Result<R> map(Function<? super T, ? extends R> fn) {
        return switch (this) {
            case Success<T> s -> Result.success(fn.apply(s.value()));
            case Failure<T> f -> Result.failure(f.notification());
        };
    }

    default <R> Result<R> flatMap(Function<? super T, Result<R>> fn) {
        return switch (this) {
            case Success<T> s -> fn.apply(s.value());
            case Failure<T> f -> Result.failure(f.notification());
        };
    }
}
