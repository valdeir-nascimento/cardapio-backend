package com.cardapio.identity.domain.model;

import com.cardapio.identity.domain.event.CustomerRegistered;
import com.cardapio.identity.domain.exception.CustomerWithoutAuthMethodException;
import com.cardapio.identity.domain.exception.SocialIdentityAlreadyLinkedException;
import com.cardapio.shared.domain.AggregateRoot;
import com.cardapio.shared.domain.Email;
import com.cardapio.shared.domain.PhoneNumber;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class Customer extends AggregateRoot<CustomerId> {

    private String name;
    private final Email email;
    private PhoneNumber phoneNumber;
    private HashedPassword passwordHash;
    private final List<SocialIdentity> socialIdentities;

    private Customer(CustomerId id, String name, Email email, PhoneNumber phoneNumber,
                     HashedPassword passwordHash, List<SocialIdentity> socialIdentities) {
        super(id);
        this.name = Objects.requireNonNull(name, "name").trim();
        if (this.name.isBlank()) throw new IllegalArgumentException("name must not be blank");
        this.email = Objects.requireNonNull(email, "email");
        this.phoneNumber = phoneNumber;
        this.passwordHash = passwordHash;
        this.socialIdentities = new ArrayList<>(socialIdentities == null ? List.of() : socialIdentities);
        enforceAtLeastOneAuthMethod();
    }

    private void enforceAtLeastOneAuthMethod() {
        if (passwordHash == null && socialIdentities.isEmpty()) {
            throw new CustomerWithoutAuthMethodException();
        }
    }

    public static Customer register(String name, Email email, PhoneNumber phoneNumber, HashedPassword passwordHash) {
        Objects.requireNonNull(phoneNumber, "phoneNumber");
        Objects.requireNonNull(passwordHash, "passwordHash");
        Customer c = new Customer(CustomerId.newId(), name, email, phoneNumber, passwordHash, List.of());
        c.registerEvent(CustomerRegistered.now(c.id(), c.email));
        return c;
    }

    public static Customer registerSocial(String name, Email email, SocialIdentity identity) {
        Objects.requireNonNull(identity, "identity");
        Customer c = new Customer(CustomerId.newId(), name, email, null, null, List.of(identity));
        c.registerEvent(CustomerRegistered.now(c.id(), c.email));
        return c;
    }

    public static Customer rehydrate(CustomerId id, String name, Email email, PhoneNumber phoneNumber, HashedPassword passwordHash) {
        return rehydrate(id, name, email, phoneNumber, passwordHash, List.of());
    }

    public static Customer rehydrate(CustomerId id, String name, Email email, PhoneNumber phoneNumber,
                                     HashedPassword passwordHash, List<SocialIdentity> socialIdentities) {
        return new Customer(id, name, email, phoneNumber, passwordHash, socialIdentities);
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

    public void linkSocialIdentity(SocialIdentity identity, Clock clock) {
        Objects.requireNonNull(identity, "identity");
        Objects.requireNonNull(clock, "clock");
        for (SocialIdentity existing : socialIdentities) {
            if (existing.provider() != identity.provider()) continue;
            if (existing.subject().equals(identity.subject())) {
                return; // idempotent — exact match, nothing to do
            }
            throw new SocialIdentityAlreadyLinkedException(identity.provider());
        }
        socialIdentities.add(identity);
    }

    public Optional<SocialIdentity> findSocialIdentity(SocialProvider provider) {
        return socialIdentities.stream().filter(s -> s.provider() == provider).findFirst();
    }

    public String name() { return name; }
    public Email email() { return email; }
    public Optional<PhoneNumber> phoneNumber() { return Optional.ofNullable(phoneNumber); }
    public Optional<HashedPassword> passwordHash() { return Optional.ofNullable(passwordHash); }
    public List<SocialIdentity> socialIdentities() { return List.copyOf(socialIdentities); }

    @Override
    public String toString() {
        return "Customer{id=" + id() + ", name='" + name + "', email=" + email.value() + "}";
    }
}
