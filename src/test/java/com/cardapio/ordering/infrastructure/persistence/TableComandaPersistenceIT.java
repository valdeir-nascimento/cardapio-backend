package com.cardapio.ordering.infrastructure.persistence;

import com.cardapio.ordering.domain.model.Comanda;
import com.cardapio.ordering.domain.model.ComandaStatus;
import com.cardapio.ordering.domain.model.OrderId;
import com.cardapio.ordering.domain.model.Table;
import com.cardapio.ordering.domain.port.ComandaRepository;
import com.cardapio.ordering.domain.port.TableRepository;
import com.cardapio.support.PostgresTestContainerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Import(PostgresTestContainerConfig.class)
class TableComandaPersistenceIT {

    @Autowired TableRepository tableRepo;
    @Autowired ComandaRepository comandaRepo;

    private final Clock clock = Clock.fixed(Instant.parse("2026-05-06T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void tableSaveAndLookupByQrToken() {
        Table table = Table.create(uniqueNumber(), clock);
        tableRepo.save(table);

        var loaded = tableRepo.findByQrToken(table.qrToken());

        assertThat(loaded).isPresent();
        assertThat(loaded.get().id()).isEqualTo(table.id());
        assertThat(loaded.get().number()).isEqualTo(table.number());
        assertThat(loaded.get().isActive()).isTrue();
    }

    @Test
    void tableUpdateAttachesQrImageKey() {
        Table table = Table.create(uniqueNumber(), clock);
        tableRepo.save(table);

        table.attachQrImageKey("tables/" + table.id().value() + ".png", clock);
        tableRepo.save(table);

        var loaded = tableRepo.findById(table.id()).orElseThrow();
        assertThat(loaded.qrImageKey()).isEqualTo("tables/" + table.id().value() + ".png");
    }

    @Test
    void comandaRoundTripPreservesCustomersAndOrders() {
        Table table = Table.create(uniqueNumber(), clock);
        tableRepo.save(table);

        UUID c1 = UUID.randomUUID();
        UUID c2 = UUID.randomUUID();
        Comanda comanda = Comanda.open(table.id(), c1, clock);
        comanda.join(c2, clock);
        OrderId orderId = OrderId.newId();
        comanda.attachOrder(orderId, clock);
        comandaRepo.save(comanda);

        var loaded = comandaRepo.findById(comanda.id()).orElseThrow();
        assertThat(loaded.tableId()).isEqualTo(table.id());
        assertThat(loaded.customerIds()).containsExactlyInAnyOrder(c1, c2);
        assertThat(loaded.orderIds()).containsExactly(orderId);
        assertThat(loaded.status()).isEqualTo(ComandaStatus.OPEN);
    }

    @Test
    void findOpenByTableIdReturnsOnlyOpen() {
        Table table = Table.create(uniqueNumber(), clock);
        tableRepo.save(table);

        Comanda c1 = Comanda.open(table.id(), UUID.randomUUID(), clock);
        c1.close(clock);
        comandaRepo.save(c1);

        Comanda c2 = Comanda.open(table.id(), UUID.randomUUID(), clock);
        comandaRepo.save(c2);

        var found = comandaRepo.findOpenByTableId(table.id());
        assertThat(found).isPresent();
        assertThat(found.get().id()).isEqualTo(c2.id());
    }

    @Test
    void uniqueIndexBlocksTwoOpenComandasPerTable() {
        Table table = Table.create(uniqueNumber(), clock);
        tableRepo.save(table);

        Comanda c1 = Comanda.open(table.id(), UUID.randomUUID(), clock);
        comandaRepo.save(c1);

        Comanda c2 = Comanda.open(table.id(), UUID.randomUUID(), clock);
        assertThatThrownBy(() -> comandaRepo.save(c2))
            .isInstanceOf(DataIntegrityViolationException.class);
    }

    private int uniqueNumber() {
        return Math.abs((int) (System.nanoTime() & 0x7FFFFFFF) % 1_000_000) + 1;
    }
}
