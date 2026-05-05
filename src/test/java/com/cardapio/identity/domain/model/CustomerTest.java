package com.cardapio.identity.domain.model;

import com.cardapio.identity.domain.event.CustomerRegistered;
import com.cardapio.shared.domain.DomainEvent;
import com.cardapio.shared.domain.Email;
import com.cardapio.shared.domain.PhoneNumber;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerTest {

    @Test
    void registersANewCustomerEmittingEvent() {
        Customer customer = Customer.register(
            "Maria Silva",
            Email.of("maria@example.com"),
            PhoneNumber.of("+5511912345678"),
            HashedPassword.of("$2a$12$dummyhash"));

        assertThat(customer.id()).isNotNull();
        assertThat(customer.name()).isEqualTo("Maria Silva");
        assertThat(customer.email().value()).isEqualTo("maria@example.com");

        List<DomainEvent> events = customer.pullDomainEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(CustomerRegistered.class);
    }

    @Test
    void changesNameAndPhone() {
        Customer customer = Customer.register(
            "Old Name",
            Email.of("a@b.com"),
            PhoneNumber.of("+5511912345678"),
            HashedPassword.of("$2a$12$x"));
        customer.pullDomainEvents();  // drain registration event

        customer.updateProfile("New Name", PhoneNumber.of("+5511987654321"));

        assertThat(customer.name()).isEqualTo("New Name");
        assertThat(customer.phoneNumber().value()).isEqualTo("+5511987654321");
    }

    @Test
    void rehydratesFromPersistence() {
        CustomerId id = CustomerId.newId();
        Customer customer = Customer.rehydrate(
            id,
            "Maria",
            Email.of("maria@example.com"),
            PhoneNumber.of("+5511912345678"),
            HashedPassword.of("$2a$12$x"));

        assertThat(customer.id()).isEqualTo(id);
        assertThat(customer.pullDomainEvents()).isEmpty();  // no event on rehydrate
    }
}
