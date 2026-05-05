package com.cardapio.shared.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ResultTest {

    @Test
    void successHoldsValue() {
        Result<String> r = Result.success("ok");
        assertThat(r.isSuccess()).isTrue();
        assertThat(r.getOrThrow()).isEqualTo("ok");
    }

    @Test
    void failureHoldsNotification() {
        Notification n = Notification.empty();
        n.addError("X", "boom");
        Result<String> r = Result.failure(n);

        assertThat(r.isSuccess()).isFalse();
        assertThat(r).isInstanceOf(Result.Failure.class);
        assertThat(((Result.Failure<String>) r).notification().errors()).hasSize(1);
    }

    @Test
    void getOrThrowOnFailureRaises() {
        Notification n = Notification.empty();
        n.addError("X", "boom");
        Result<String> r = Result.failure(n);

        assertThatThrownBy(r::getOrThrow).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void mapTransformsSuccess() {
        Result<Integer> r = Result.<String>success("42").map(Integer::parseInt);
        assertThat(r.getOrThrow()).isEqualTo(42);
    }

    @Test
    void mapPropagatesFailure() {
        Notification n = Notification.empty();
        n.addError("X", "boom");
        Result<Integer> r = Result.<String>failure(n).map(Integer::parseInt);
        assertThat(r.isSuccess()).isFalse();
    }

    @Test
    void flatMapChainsResults() {
        Result<String> r = Result.<Integer>success(10)
            .flatMap(i -> Result.success("v=" + i));
        assertThat(r.getOrThrow()).isEqualTo("v=10");
    }
}
