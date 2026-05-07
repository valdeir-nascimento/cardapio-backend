package com.cardapio.review.infrastructure.persistence.repository;

import com.cardapio.review.infrastructure.persistence.jpa.ReviewableOrderJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SpringReviewableOrderJpaRepository extends JpaRepository<ReviewableOrderJpaEntity, UUID> {

    /**
     * Reviewable orders for a customer that don't yet have a review row in
     * the reviews table. Done via NOT EXISTS so the join is index-friendly.
     */
    @Query(value = """
        select r from ReviewableOrderJpaEntity r
        where r.customerId = :customerId
        and not exists (
            select 1 from ReviewJpaEntity rv
            where rv.orderId = r.orderId and rv.customerId = :customerId
        )
        order by r.terminalAt desc
        """)
    List<ReviewableOrderJpaEntity> findPendingByCustomerId(@Param("customerId") UUID customerId, Pageable pageable);
}
