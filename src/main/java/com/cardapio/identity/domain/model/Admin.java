package com.cardapio.identity.domain.model;

import com.cardapio.shared.domain.AggregateRoot;
import com.cardapio.shared.domain.Email;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

public final class Admin extends AggregateRoot<AdminId> {

    private String name;
    private final Email email;
    private HashedPassword passwordHash;
    private final EnumSet<Role> roles;

    private Admin(AdminId id, String name, Email email, HashedPassword passwordHash, Set<Role> roles) {
        super(id);
        this.name = Objects.requireNonNull(name, "name").trim();
        if (this.name.isBlank()) throw new IllegalArgumentException("name must not be blank");
        this.email = Objects.requireNonNull(email, "email");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
        Objects.requireNonNull(roles, "roles");
        if (roles.isEmpty()) throw new IllegalArgumentException("admin must have at least one role");
        this.roles = EnumSet.copyOf(roles);
    }

    public static Admin create(String name, Email email, HashedPassword passwordHash, Set<Role> roles) {
        return new Admin(AdminId.newId(), name, email, passwordHash, roles);
    }

    public static Admin rehydrate(AdminId id, String name, Email email, HashedPassword passwordHash, Set<Role> roles) {
        return new Admin(id, name, email, passwordHash, roles);
    }

    public boolean hasRole(Role role) { return roles.contains(role); }
    public Set<Role> roles() { return java.util.Collections.unmodifiableSet(roles); }
    public String name() { return name; }
    public Email email() { return email; }
    public HashedPassword passwordHash() { return passwordHash; }

    @Override
    public String toString() {
        return "Admin{id=" + id() + ", name='" + name + "', email=" + email.value() + ", roles=" + roles + "}";
    }
}
