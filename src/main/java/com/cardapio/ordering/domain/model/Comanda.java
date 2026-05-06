package com.cardapio.ordering.domain.model;

import com.cardapio.ordering.domain.event.ComandaClosed;
import com.cardapio.ordering.domain.event.ComandaJoined;
import com.cardapio.ordering.domain.event.ComandaOpened;
import com.cardapio.ordering.domain.exception.DineInInvariantException;
import com.cardapio.shared.domain.AggregateRoot;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class Comanda extends AggregateRoot<ComandaId> {

    private final TableId tableId;
    private final Set<UUID> customerIds;
    private final List<OrderId> orderIds;
    private ComandaStatus status;
    private final Instant openedAt;
    private Instant closedAt;
    private Instant updatedAt;

    private Comanda(ComandaId id, TableId tableId, Set<UUID> customerIds, List<OrderId> orderIds,
                    ComandaStatus status, Instant openedAt, Instant closedAt, Instant updatedAt) {
        super(id);
        this.tableId = Objects.requireNonNull(tableId, "tableId");
        this.customerIds = new LinkedHashSet<>(Objects.requireNonNull(customerIds, "customerIds"));
        this.orderIds = new ArrayList<>(Objects.requireNonNull(orderIds, "orderIds"));
        this.status = Objects.requireNonNull(status, "status");
        this.openedAt = Objects.requireNonNull(openedAt, "openedAt");
        this.closedAt = closedAt;
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public static Comanda open(TableId tableId, UUID openerCustomerId, Clock clock) {
        Objects.requireNonNull(openerCustomerId, "openerCustomerId");
        Instant now = clock.instant();
        ComandaId id = ComandaId.newId();
        Set<UUID> customers = new LinkedHashSet<>();
        customers.add(openerCustomerId);
        Comanda c = new Comanda(id, tableId, customers, new ArrayList<>(), ComandaStatus.OPEN, now, null, now);
        c.registerEvent(ComandaOpened.of(id, tableId, openerCustomerId, now));
        return c;
    }

    public static Comanda rehydrate(ComandaId id, TableId tableId, Set<UUID> customerIds,
                                    List<OrderId> orderIds, ComandaStatus status,
                                    Instant openedAt, Instant closedAt, Instant updatedAt) {
        return new Comanda(id, tableId, customerIds, orderIds, status, openedAt, closedAt, updatedAt);
    }

    public void join(UUID customerId, Clock clock) {
        Objects.requireNonNull(customerId, "customerId");
        if (status == ComandaStatus.CLOSED) {
            throw new DineInInvariantException("cannot join a closed comanda");
        }
        boolean added = customerIds.add(customerId);
        if (added) {
            Instant now = clock.instant();
            this.updatedAt = now;
            registerEvent(ComandaJoined.of(id(), customerId, now));
        }
    }

    public void attachOrder(OrderId orderId, Clock clock) {
        Objects.requireNonNull(orderId, "orderId");
        if (status == ComandaStatus.CLOSED) {
            throw new DineInInvariantException("cannot attach order to a closed comanda");
        }
        if (!orderIds.contains(orderId)) {
            orderIds.add(orderId);
            this.updatedAt = clock.instant();
        }
    }

    public boolean hasMember(UUID customerId) {
        return customerIds.contains(customerId);
    }

    public void close(Clock clock) {
        if (status == ComandaStatus.CLOSED) {
            return;
        }
        Instant now = clock.instant();
        this.status = ComandaStatus.CLOSED;
        this.closedAt = now;
        this.updatedAt = now;
        registerEvent(ComandaClosed.of(id(), tableId, now));
    }

    public TableId tableId() { return tableId; }
    public Set<UUID> customerIds() { return Set.copyOf(customerIds); }
    public List<OrderId> orderIds() { return List.copyOf(orderIds); }
    public ComandaStatus status() { return status; }
    public Instant openedAt() { return openedAt; }
    public Instant closedAt() { return closedAt; }
    public Instant updatedAt() { return updatedAt; }
}
