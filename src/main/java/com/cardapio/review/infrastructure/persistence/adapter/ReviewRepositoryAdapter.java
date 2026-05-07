package com.cardapio.review.infrastructure.persistence.adapter;

import com.cardapio.ordering.domain.model.OrderId;
import com.cardapio.review.domain.model.Review;
import com.cardapio.review.domain.model.ReviewId;
import com.cardapio.review.domain.port.ReviewRepository;
import com.cardapio.review.infrastructure.persistence.mapper.ReviewMapper;
import com.cardapio.review.infrastructure.persistence.repository.SpringReviewJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ReviewRepositoryAdapter implements ReviewRepository {

    private final SpringReviewJpaRepository jpa;

    @Override
    public void save(Review review) {
        jpa.save(ReviewMapper.toJpa(review));
    }

    @Override
    public Optional<Review> findById(ReviewId id) {
        return jpa.findById(id.value()).map(ReviewMapper::toDomain);
    }

    @Override
    public Optional<Review> findByOrderIdAndCustomerId(OrderId orderId, UUID customerId) {
        return jpa.findByOrderIdAndCustomerId(orderId.value(), customerId).map(ReviewMapper::toDomain);
    }

    @Override
    public boolean existsByOrderIdAndCustomerId(OrderId orderId, UUID customerId) {
        return jpa.existsByOrderIdAndCustomerId(orderId.value(), customerId);
    }
}
