package com.cardapio.review.infrastructure.persistence.repository;

import com.cardapio.review.infrastructure.persistence.jpa.ReviewJpaEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringReviewJpaRepository extends JpaRepository<ReviewJpaEntity, UUID> {
    Optional<ReviewJpaEntity> findByOrderIdAndCustomerId(UUID orderId, UUID customerId);
    boolean existsByOrderIdAndCustomerId(UUID orderId, UUID customerId);
    List<ReviewJpaEntity> findAllByCustomerIdOrderByCreatedAtDesc(UUID customerId, Pageable pageable);

    @Query(value = """
        SELECT rop.product_id   AS productId,
               AVG(r.rating)    AS avgRating,
               COUNT(*)         AS reviewCount
        FROM reviews r
        JOIN reviewable_order_products rop ON rop.order_id = r.order_id
        WHERE (CAST(:productId AS uuid) IS NULL OR rop.product_id = CAST(:productId AS uuid))
          AND (CAST(:fromTs AS timestamptz) IS NULL OR r.created_at >= CAST(:fromTs AS timestamptz))
          AND (CAST(:toTs   AS timestamptz) IS NULL OR r.created_at <= CAST(:toTs   AS timestamptz))
        GROUP BY rop.product_id
        ORDER BY reviewCount DESC
        LIMIT :lim OFFSET :off
        """, nativeQuery = true)
    List<ProductRatingStatsRow> findProductRatingStats(
        @Param("productId") UUID productId,
        @Param("fromTs") Instant from,
        @Param("toTs") Instant to,
        @Param("lim") int limit,
        @Param("off") int offset
    );
}
