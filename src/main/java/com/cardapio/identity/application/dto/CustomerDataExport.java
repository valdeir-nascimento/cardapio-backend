package com.cardapio.identity.application.dto;

import com.cardapio.identity.domain.model.SocialProvider;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * LGPD data export of identity-owned data for a customer.
 *
 * Order history and reviews are intentionally not aggregated here — they are
 * already reachable through the customer's authenticated session
 * (GET /me/orders, GET /me/orders/{id}/review). Aggregating across modules
 * would create a cycle (ordering and review already depend on identity).
 * The front-end stitches the full export from these calls.
 */
public record CustomerDataExport(
    UUID id,
    String name,
    String email,
    String phoneNumber,
    List<SocialIdentitySummary> socialIdentities,
    Instant deletedAt,
    Instant exportedAt
) {
    public record SocialIdentitySummary(SocialProvider provider, String emailAtLink, Instant linkedAt) {}
}
