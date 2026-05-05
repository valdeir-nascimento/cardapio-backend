package com.cardapio.identity.domain.model;

import com.cardapio.identity.domain.event.CustomerRegistered;
import com.cardapio.shared.domain.AggregateRoot;
import com.cardapio.shared.domain.Email;
import com.cardapio.shared.domain.PhoneNumber;

import java.util.Objects;

public final class Customer extends AggregateRoot<CustomerId> {

    private String name;
    private final Email email;
    private PhoneNumber phoneNumber;
    private HashedPassword passwordHash;

    private Customer(CustomerId id, String name, Email email, PhoneNumber phoneNumber, HashedPassword passwordHash) {
        super(id);
        this.name = Objects.requireNonNull(name, "name").trim();
        if (this.name.isBlank()) throw new IllegalArgumentException("name must not be blank");
        this.email = Objects.requireNonNull(email, "email");
        this.phoneNumber = Objects.requireNonNull(phoneNumber, "phoneNumber");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
    }

    public static Customer register(String name, Email email, PhoneNumber phoneNumber, HashedPassword passwordHash) {
        Customer c = new Customer(CustomerId.newId(), name, email, phoneNumber, passwordHash);
        c.registerEvent(CustomerRegistered.now(c.id(), c.email));
        return c;
    }

    public static Customer rehydrate(CustomerId id, String name, Email email, PhoneNumber phoneNumber, HashedPassword passwordHash) {
        return new Customer(id, name, email, phoneNumber, passwordHash);
    }

    public void updateProfile(String name, PhoneNumber phoneNumber) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(phoneNumber, "phoneNumber");
        String trimmed = name.trim();
        if (trimmed.isBlank()) throw new IllegalArgumentException("name must not be blank");
        this.name = trimmed;
        this.phoneNumber = phoneNumber;
    }

    public void changePassword(HashedPassword newHash) {
        this.passwordHash = Objects.requireNonNull(newHash, "newHash");
    }

    public String name() { return name; }
    public Email email() { return email; }
    public PhoneNumber phoneNumber() { return phoneNumber; }
    public HashedPassword passwordHash() { return passwordHash; }
}
