package com.cardapio.identity.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenTest {

    @Test
    void issuesActiveTokenForCustomer() {
        UUID subject = UUID.randomUUID();
        Instant now = Instant.parse("2026-05-04T10:00:00Z");
        RefreshToken token = RefreshToken.issue(
            subject, Audience.CUSTOMER, "hashed-token", now, Duration.ofDays(30));

        assertThat(token.subject()).isEqualTo(subject);
        assertThat(token.audience()).isEqualTo(Audience.CUSTOMER);
        assertThat(token.isRevoked()).isFalse();
        assertThat(token.expiresAt()).isEqualTo(now.plus(Duration.ofDays(30)));
        assertThat(token.isActiveAt(now.plusSeconds(60))).isTrue();
    }

    @Test
    void detectsExpiry() {
        Instant now = Instant.parse("2026-05-04T10:00:00Z");
        RefreshToken token = RefreshToken.issue(
            UUID.randomUUID(), Audience.CUSTOMER, "h", now, Duration.ofMinutes(5));

        assertThat(token.isActiveAt(now.plusSeconds(60))).isTrue();
        assertThat(token.isActiveAt(now.plus(Duration.ofMinutes(10)))).isFalse();
    }

    @Test
    void revokeMakesTokenInactive() {
        Instant now = Instant.parse("2026-05-04T10:00:00Z");
        RefreshToken token = RefreshToken.issue(
            UUID.randomUUID(), Audience.CUSTOMER, "h", now, Duration.ofDays(30));

        token.revoke();

        assertThat(token.isRevoked()).isTrue();
        assertThat(token.isActiveAt(now.plusSeconds(60))).isFalse();
    }
}
