package com.cardapio.review.application.usecase;

import com.cardapio.review.application.dto.ReviewableOrderView;
import com.cardapio.review.domain.port.ReviewableOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetMyReviewableOrdersUseCase {

    private final ReviewableOrderRepository reviewableOrders;

    @Transactional(readOnly = true)
    public List<ReviewableOrderView> execute(UUID customerId, int limit, int offset) {
        return reviewableOrders.findPendingByCustomerId(customerId, limit, offset).stream()
            .map(ReviewableOrderView::from).toList();
    }
}
