package com.cardapio.identity.application.usecase;

import com.cardapio.identity.domain.model.Audience;
import com.cardapio.identity.domain.model.Customer;
import com.cardapio.identity.domain.model.HashedPassword;
import com.cardapio.identity.domain.model.Role;
import com.cardapio.identity.domain.port.*;
import com.cardapio.shared.domain.Email;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class LoginCustomerUseCase extends AbstractLoginUseCase<Customer> {

    private final CustomerRepository customers;

    public LoginCustomerUseCase(
        CustomerRepository customers,
        PasswordHasher hasher,
        JwtIssuer issuer,
        RefreshTokenRepository refreshTokens,
        TokenHasher tokenHasher,
        Clock clock
    ) {
        super(hasher, issuer, refreshTokens, tokenHasher, clock);
        this.customers = customers;
    }

    @Override
    @Transactional
    protected Optional<Customer> findUser(Email email) {
        return customers.findByEmail(email).filter(c -> !c.isDeleted());
    }

    @Override
    protected Optional<HashedPassword> passwordOf(Customer u) {
        return u.passwordHash();
    }

    @Override
    protected UUID subjectOf(Customer u) {
        return u.id().value();
    }

    @Override
    protected Audience audienceOf(Customer u) {
        return Audience.CUSTOMER;
    }

    @Override
    protected Set<Role> rolesOf(Customer u) {
        return Set.of();
    }
}
