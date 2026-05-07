package com.cardapio.identity.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "customer_social_identities")
@IdClass(SocialIdentityJpaEntity.PK.class)
public class SocialIdentityJpaEntity {

    @Id @Column(name = "customer_id", nullable = false) private UUID customerId;
    @Id @Column(name = "provider", nullable = false, length = 20) private String provider;
    @Column(name = "subject", nullable = false, length = 200) private String subject;
    @Column(name = "email_at_link", nullable = false, length = 180) private String emailAtLink;
    @Column(name = "linked_at", nullable = false) private Instant linkedAt;

    protected SocialIdentityJpaEntity() {}

    public SocialIdentityJpaEntity(UUID customerId, String provider, String subject,
                                   String emailAtLink, Instant linkedAt) {
        this.customerId = customerId;
        this.provider = provider;
        this.subject = subject;
        this.emailAtLink = emailAtLink;
        this.linkedAt = linkedAt;
    }

    public UUID getCustomerId() { return customerId; }
    public String getProvider() { return provider; }
    public String getSubject() { return subject; }
    public String getEmailAtLink() { return emailAtLink; }
    public Instant getLinkedAt() { return linkedAt; }

    public static class PK implements Serializable {
        private UUID customerId;
        private String provider;

        public PK() {}
        public PK(UUID customerId, String provider) {
            this.customerId = customerId;
            this.provider = provider;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PK pk)) return false;
            return Objects.equals(customerId, pk.customerId) && Objects.equals(provider, pk.provider);
        }

        @Override
        public int hashCode() { return Objects.hash(customerId, provider); }
    }
}
