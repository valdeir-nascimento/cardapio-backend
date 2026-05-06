package com.cardapio.notification.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationOutboxTest {

    private final Instant t0 = Instant.parse("2026-05-06T10:00:00Z");
    private final Clock clock = Clock.fixed(t0, ZoneOffset.UTC);
    private final UUID recipient = UUID.randomUUID();

    @Test
    void enqueueStartsPendingDueImmediately() {
        NotificationOutbox box = NotificationOutbox.enqueue(
            NotificationChannel.EMAIL, NotificationTemplate.ORDER_RECEIVED, recipient, "{}", clock);

        assertThat(box.status()).isEqualTo(NotificationStatus.PENDING);
        assertThat(box.attempts()).isZero();
        assertThat(box.scheduledFor()).isEqualTo(t0);
        assertThat(box.isDue(t0)).isTrue();
    }

    @Test
    void cannotEnqueueSseChannel() {
        assertThatThrownBy(() -> NotificationOutbox.enqueue(
            NotificationChannel.SSE_ADMIN, NotificationTemplate.ORDER_RECEIVED, recipient, "{}", clock))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void markSentTransitionsToTerminal() {
        NotificationOutbox box = NotificationOutbox.enqueue(
            NotificationChannel.EMAIL, NotificationTemplate.ORDER_RECEIVED, recipient, "{}", clock);
        box.markSent(clock);

        assertThat(box.status()).isEqualTo(NotificationStatus.SENT);
        assertThat(box.attempts()).isEqualTo(1);
        assertThat(box.lastError()).isNull();
        assertThatThrownBy(() -> box.markSent(clock)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void markFailedReschedulesWithBackoff() {
        NotificationOutbox box = NotificationOutbox.enqueue(
            NotificationChannel.WHATSAPP, NotificationTemplate.PAYMENT_REJECTED, recipient, "{}", clock);

        box.markFailed("boom", clock);
        assertThat(box.status()).isEqualTo(NotificationStatus.FAILED);
        assertThat(box.attempts()).isEqualTo(1);
        assertThat(box.lastError()).isEqualTo("boom");
        assertThat(box.scheduledFor()).isEqualTo(t0.plus(Duration.ofMinutes(1)));

        Clock c2 = Clock.fixed(t0.plus(Duration.ofMinutes(1)), ZoneOffset.UTC);
        box.markFailed("boom2", c2);
        assertThat(box.attempts()).isEqualTo(2);
        assertThat(box.scheduledFor()).isEqualTo(t0.plus(Duration.ofMinutes(1)).plus(Duration.ofMinutes(5)));
    }

    @Test
    void abandonsAfterMaxAttempts() {
        NotificationOutbox box = NotificationOutbox.enqueue(
            NotificationChannel.EMAIL, NotificationTemplate.ORDER_RECEIVED, recipient, "{}", clock);

        for (int i = 0; i < NotificationOutbox.MAX_ATTEMPTS; i++) {
            box.markFailed("error " + i, clock);
        }
        assertThat(box.status()).isEqualTo(NotificationStatus.ABANDONED);
        assertThat(box.attempts()).isEqualTo(NotificationOutbox.MAX_ATTEMPTS);
        assertThatThrownBy(() -> box.markFailed("again", clock)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void isDueRespectsScheduledFor() {
        NotificationOutbox box = NotificationOutbox.enqueue(
            NotificationChannel.EMAIL, NotificationTemplate.ORDER_RECEIVED, recipient, "{}", clock);
        box.markFailed("boom", clock);

        assertThat(box.isDue(t0)).isFalse();
        assertThat(box.isDue(t0.plus(Duration.ofSeconds(59)))).isFalse();
        assertThat(box.isDue(t0.plus(Duration.ofMinutes(1)))).isTrue();
    }

    @Test
    void terminalStatesAreNotDue() {
        NotificationOutbox box = NotificationOutbox.enqueue(
            NotificationChannel.EMAIL, NotificationTemplate.ORDER_RECEIVED, recipient, "{}", clock);
        box.markSent(clock);
        assertThat(box.isDue(t0.plus(Duration.ofDays(1)))).isFalse();
    }
}
