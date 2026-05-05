package com.cardapio.identity.infrastructure.persistence.jpa;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "admins")
public class AdminJpaEntity {

    @Id
    @Column(nullable = false)
    private UUID id;
    @Column(nullable = false, length = 120)
    private String name;
    @Column(nullable = false, length = 180, unique = true)
    private String email;
    @Column(name = "password_hash", nullable = false, length = 120)
    private String passwordHash;
    @Column(nullable = false, length = 60)
    private String roles;  // comma-separated
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AdminJpaEntity() {
    }

    public AdminJpaEntity(UUID id, String name, String email, String passwordHash, String roles, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.roles = roles;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getRoles() {
        return roles;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void setRoles(String roles) {
        this.roles = roles;
    }
}
