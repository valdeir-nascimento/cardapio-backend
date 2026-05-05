package com.cardapio.identity.infrastructure.security;

import com.cardapio.identity.domain.model.Audience;
import com.cardapio.identity.domain.model.Role;
import com.cardapio.identity.domain.port.JwtIssuer;
import com.cardapio.identity.domain.port.JwtVerifier;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JjwtAdapterTest {

    private final JwtProperties props = new JwtProperties(
        "test-secret-must-be-long-enough-for-hs256-32bytes!!",
        Duration.ofMinutes(15),
        Duration.ofDays(30));
    private final Clock fixedClock = Clock.fixed(Instant.parse("2026-05-04T10:00:00Z"), ZoneId.of("UTC"));
    private final JjwtAdapter adapter = new JjwtAdapter(props, fixedClock);

    @Test
    void issuesAndVerifiesCustomerToken() {
        UUID subject = UUID.randomUUID();
        JwtIssuer.IssuedJwt issued = adapter.issueAccessToken(subject, Audience.CUSTOMER, Set.of());

        JwtVerifier.VerifiedJwt verified = adapter.verify(issued.token());

        assertThat(verified.subject()).isEqualTo(subject);
        assertThat(verified.audience()).isEqualTo(Audience.CUSTOMER);
        assertThat(verified.roles()).isEmpty();
    }

    @Test
    void issuesAdminTokenWithRoles() {
        UUID subject = UUID.randomUUID();
        JwtIssuer.IssuedJwt issued = adapter.issueAccessToken(subject, Audience.ADMIN, Set.of(Role.OWNER, Role.MANAGER));

        JwtVerifier.VerifiedJwt verified = adapter.verify(issued.token());

        assertThat(verified.audience()).isEqualTo(Audience.ADMIN);
        assertThat(verified.roles()).containsExactlyInAnyOrder(Role.OWNER, Role.MANAGER);
    }

    @Test
    void rejectsTamperedToken() {
        JwtIssuer.IssuedJwt issued = adapter.issueAccessToken(UUID.randomUUID(), Audience.CUSTOMER, Set.of());
        String tampered = issued.token().substring(0, issued.token().length() - 4) + "XXXX";

        assertThatThrownBy(() -> adapter.verify(tampered))
            .hasMessageContaining("invalid");
    }

    @Test
    void generatesUniqueRefreshTokens() {
        String t1 = adapter.generateOpaqueRefreshToken();
        String t2 = adapter.generateOpaqueRefreshToken();
        assertThat(t1).isNotEqualTo(t2);
        assertThat(t1.length()).isGreaterThanOrEqualTo(43);  // 32 bytes base64url ≈ 43 chars
    }
}
