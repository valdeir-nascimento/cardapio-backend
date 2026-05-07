package com.cardapio.identity.infrastructure.persistence.repository;

import com.cardapio.identity.infrastructure.persistence.jpa.CustomerJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SpringCustomerJpaRepository extends JpaRepository<CustomerJpaEntity, UUID> {
    Optional<CustomerJpaEntity> findByEmail(String email);
    boolean existsByEmail(String email);

    @Query("""
        select c from CustomerJpaEntity c
        join c.socialIdentities s
        where s.provider = :provider and s.subject = :subject
        """)
    Optional<CustomerJpaEntity> findBySocialIdentity(@Param("provider") String provider,
                                                     @Param("subject") String subject);
}
