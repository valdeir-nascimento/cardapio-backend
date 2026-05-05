package com.cardapio.identity.infrastructure.persistence.mapper;

import com.cardapio.identity.domain.model.Admin;
import com.cardapio.identity.domain.model.AdminId;
import com.cardapio.identity.domain.model.HashedPassword;
import com.cardapio.identity.domain.model.Role;
import com.cardapio.identity.infrastructure.persistence.jpa.AdminJpaEntity;
import com.cardapio.shared.domain.Email;

import java.time.Instant;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

public final class AdminMapper {
    private AdminMapper() {}

    public static AdminJpaEntity toJpa(Admin a, Instant now) {
        String roles = a.roles().stream().map(Role::name).collect(Collectors.joining(","));
        return new AdminJpaEntity(a.id().value(), a.name(), a.email().value(), a.passwordHash().value(), roles, now);
    }

    public static void updateJpa(AdminJpaEntity entity, Admin a) {
        entity.setName(a.name());
        entity.setPasswordHash(a.passwordHash().value());
        entity.setRoles(a.roles().stream().map(Role::name).collect(Collectors.joining(",")));
    }

    public static Admin toDomain(AdminJpaEntity e) {
        Set<Role> roles = e.getRoles().isBlank()
            ? EnumSet.noneOf(Role.class)
            : Arrays.stream(e.getRoles().split(",")).map(Role::valueOf).collect(Collectors.toCollection(() -> EnumSet.noneOf(Role.class)));
        return Admin.rehydrate(AdminId.of(e.getId()), e.getName(), Email.of(e.getEmail()), new HashedPassword(e.getPasswordHash()), roles);
    }
}
