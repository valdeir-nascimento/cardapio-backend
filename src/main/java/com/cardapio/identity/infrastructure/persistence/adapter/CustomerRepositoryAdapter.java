package com.cardapio.identity.infrastructure.persistence.adapter;

import com.cardapio.identity.domain.model.Customer;
import com.cardapio.identity.domain.model.CustomerId;
import com.cardapio.identity.domain.port.CustomerRepository;
import com.cardapio.identity.infrastructure.persistence.mapper.CustomerMapper;
import com.cardapio.identity.infrastructure.persistence.repository.SpringCustomerJpaRepository;
import com.cardapio.shared.domain.Email;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Optional;

@Component
public class CustomerRepositoryAdapter implements CustomerRepository {

    private final SpringCustomerJpaRepository jpa;
    private final Clock clock;

    public CustomerRepositoryAdapter(SpringCustomerJpaRepository jpa, Clock clock) {
        this.jpa = jpa;
        this.clock = clock;
    }

    @Override
    public void save(Customer customer) {
        var existing = jpa.findById(customer.id().value());
        if (existing.isPresent()) {
            CustomerMapper.updateJpa(existing.get(), customer, clock.instant());
            jpa.save(existing.get());
        } else {
            jpa.save(CustomerMapper.toJpa(customer, clock.instant()));
        }
    }

    @Override
    public Optional<Customer> findById(CustomerId id) {
        return jpa.findById(id.value()).map(CustomerMapper::toDomain);
    }

    @Override
    public Optional<Customer> findByEmail(Email email) {
        return jpa.findByEmail(email.value()).map(CustomerMapper::toDomain);
    }

    @Override
    public boolean existsByEmail(Email email) {
        return jpa.existsByEmail(email.value());
    }
}
