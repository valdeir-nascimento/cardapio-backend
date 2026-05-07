package com.cardapio.review.application.usecase;

import com.cardapio.ordering.domain.model.OrderId;
import com.cardapio.review.application.dto.ReviewView;
import com.cardapio.review.domain.port.ReviewRepository;
import com.cardapio.shared.domain.ErrorCode;
import com.cardapio.shared.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetMyReviewUseCase {

    private final ReviewRepository reviews;

    @Transactional(readOnly = true)
    public Result<ReviewView> execute(OrderId orderId, UUID customerId) {
        return reviews.findByOrderIdAndCustomerId(orderId, customerId)
            .map(r -> Result.success(ReviewView.from(r)))
            .orElseGet(() -> Result.failWith(ErrorCode.REVIEW_NOT_FOUND));
    }
}
