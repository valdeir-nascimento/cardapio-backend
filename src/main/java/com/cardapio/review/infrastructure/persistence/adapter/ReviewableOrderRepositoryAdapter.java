package com.cardapio.review.infrastructure.persistence.adapter;

import com.cardapio.ordering.domain.model.OrderId;
import com.cardapio.review.domain.model.ReviewableOrder;
import com.cardapio.review.domain.port.ReviewableOrderRepository;
import com.cardapio.review.infrastructure.persistence.mapper.ReviewableOrderMapper;
import com.cardapio.review.infrastructure.persistence.repository.SpringReviewableOrderJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ReviewableOrderRepositoryAdapter implements ReviewableOrderRepository {

    private final SpringReviewableOrderJpaRepository jpa;
    private final Clock clock;

    @Override
    public void save(ReviewableOrder reviewable) {
        jpa.save(ReviewableOrderMapper.toJpa(reviewable, clock));
    }

    @Override
    public void deleteByOrderId(OrderId orderId) {
        jpa.deleteById(orderId.value());
    }

    @Override
    public Optional<ReviewableOrder> findByOrderId(OrderId orderId) {
        return jpa.findById(orderId.value()).map(ReviewableOrderMapper::toDomain);
    }

    @Override
    public List<ReviewableOrder> findPendingByCustomerId(UUID customerId, int limit, int offset) {
        int page = offset / Math.max(1, limit);
        return jpa.findPendingByCustomerId(customerId, PageRequest.of(page, limit))
            .stream().map(ReviewableOrderMapper::toDomain).toList();
    }
}
