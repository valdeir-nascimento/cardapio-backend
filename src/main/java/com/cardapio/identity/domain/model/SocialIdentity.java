package com.cardapio.identity.domain.model;

import java.time.Instant;
import java.util.Objects;

public record SocialIdentity(SocialProvider provider, String subject, String emailAtLink, Instant linkedAt) {
    public SocialIdentity {
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(subject, "subject");
        if (subject.isBlank()) throw new IllegalArgumentException("subject must not be blank");
        Objects.requireNonNull(emailAtLink, "emailAtLink");
        Objects.requireNonNull(linkedAt, "linkedAt");
    }
}
