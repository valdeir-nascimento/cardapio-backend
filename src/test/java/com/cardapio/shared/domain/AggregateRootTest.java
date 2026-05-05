package com.cardapio.shared.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AggregateRootTest {

    record SampleEvent(UUID id, Instant occurredOn) implements DomainEvent {}

    static class SampleAggregate extends AggregateRoot<UUID> {
        SampleAggregate(UUID id) { super(id); }
        void doSomething() {
            registerEvent(new SampleEvent(UUID.randomUUID(), Instant.now()));
        }
    }

    @Test
    void registersAndExposesPendingEvents() {
        SampleAggregate aggregate = new SampleAggregate(UUID.randomUUID());

        aggregate.doSomething();
        aggregate.doSomething();

        List<DomainEvent> events = aggregate.pullDomainEvents();
        assertThat(events).hasSize(2);
        assertThat(aggregate.pullDomainEvents()).isEmpty();  // pulled, now drained
    }

    @Test
    void aggregatesWithSameIdAreEqual() {
        UUID id = UUID.randomUUID();
        SampleAggregate a = new SampleAggregate(id);
        SampleAggregate b = new SampleAggregate(id);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }
}
