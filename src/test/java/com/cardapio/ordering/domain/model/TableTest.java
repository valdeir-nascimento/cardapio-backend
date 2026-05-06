package com.cardapio.ordering.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TableTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-05-06T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void createGeneratesIdAndQrToken() {
        Table table = Table.create(7, clock);

        assertThat(table.id()).isNotNull();
        assertThat(table.qrToken()).isNotNull();
        assertThat(table.number()).isEqualTo(7);
        assertThat(table.isActive()).isTrue();
        assertThat(table.qrImageKey()).isNull();
        assertThat(table.createdAt()).isEqualTo(clock.instant());
        assertThat(table.updatedAt()).isEqualTo(clock.instant());
    }

    @Test
    void rejectsNonPositiveNumber() {
        assertThatThrownBy(() -> Table.create(0, clock))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Table.create(-1, clock))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void qrTokenIsImmutableAcrossOps() {
        Table table = Table.create(1, clock);
        UUID original = table.qrToken();

        table.attachQrImageKey("tables/key.png", clock);
        table.deactivate(clock);
        table.activate(clock);

        assertThat(table.qrToken()).isEqualTo(original);
    }

    @Test
    void attachQrImageKeyRejectsBlank() {
        Table table = Table.create(1, clock);
        assertThatThrownBy(() -> table.attachQrImageKey("", clock))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> table.attachQrImageKey("  ", clock))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deactivateAndActivateUpdateState() {
        Table table = Table.create(1, clock);

        table.deactivate(clock);
        assertThat(table.isActive()).isFalse();

        table.activate(clock);
        assertThat(table.isActive()).isTrue();
    }
}
