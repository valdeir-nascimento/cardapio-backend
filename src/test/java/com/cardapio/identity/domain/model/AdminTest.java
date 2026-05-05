package com.cardapio.identity.domain.model;

import com.cardapio.shared.domain.Email;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdminTest {

    @Test
    void createsAdminWithRoles() {
        Admin admin = Admin.create(
            "Boss",
            Email.of("boss@cardapio.com"),
            HashedPassword.of("$2a$12$x"),
            Set.of(Role.OWNER));

        assertThat(admin.name()).isEqualTo("Boss");
        assertThat(admin.hasRole(Role.OWNER)).isTrue();
        assertThat(admin.hasRole(Role.OPERATOR)).isFalse();
    }

    @Test
    void rolesAreImmutableFromOutside() {
        Admin admin = Admin.create(
            "X", Email.of("x@y.com"), HashedPassword.of("$2a$12$x"), Set.of(Role.MANAGER));

        Set<Role> roles = admin.roles();
        assertThatThrownBy(() -> roles.add(Role.OWNER))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsEmptyRoles() {
        assertThatThrownBy(() -> Admin.create(
            "X", Email.of("x@y.com"), HashedPassword.of("$2a$12$x"), Set.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("at least one role");
    }
}
