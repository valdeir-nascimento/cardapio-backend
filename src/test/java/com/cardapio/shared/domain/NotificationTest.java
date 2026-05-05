package com.cardapio.shared.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationTest {

    @Test
    void emptyHasNoErrors() {
        Notification n = Notification.empty();
        assertThat(n.hasErrors()).isFalse();
        assertThat(n.errors()).isEmpty();
    }

    @Test
    void addsErrorsAndExposesThem() {
        Notification n = Notification.empty();
        n.addError("OUT_OF_STOCK", "produto esgotado");
        n.addError("address", "OUT_OF_AREA", "fora de área");

        assertThat(n.hasErrors()).isTrue();
        assertThat(n.errors()).containsExactly(
            new NotificationError(null, "OUT_OF_STOCK", "produto esgotado"),
            new NotificationError("address", "OUT_OF_AREA", "fora de área"));
    }

    @Test
    void errorsListIsImmutable() {
        Notification n = Notification.empty();
        n.addError("X", "msg");
        assertThatThrownBy(() -> n.errors().add(new NotificationError(null, "Y", "m")))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void mergeAddsErrorsFromAnotherNotification() {
        Notification a = Notification.empty();
        a.addError("A", "m1");
        Notification b = Notification.empty();
        b.addError("B", "m2");

        a.merge(b);
        assertThat(a.errors()).hasSize(2);
    }
}
