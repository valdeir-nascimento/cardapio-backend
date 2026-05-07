package com.cardapio.identity.domain.port;

import com.cardapio.identity.domain.model.Customer;
import com.cardapio.identity.domain.model.CustomerId;
import com.cardapio.identity.domain.model.SocialProvider;
import com.cardapio.shared.domain.Email;

import java.util.Optional;

public interface CustomerRepository {
    void save(Customer customer);
    Optional<Customer> findById(CustomerId id);
    Optional<Customer> findByEmail(Email email);
    boolean existsByEmail(Email email);
    Optional<Customer> findBySocialIdentity(SocialProvider provider, String subject);
}
