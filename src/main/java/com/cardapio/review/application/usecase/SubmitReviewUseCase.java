package com.cardapio.review.application.usecase;

import com.cardapio.review.application.command.SubmitReviewCommand;
import com.cardapio.review.application.dto.ReviewView;
import com.cardapio.review.domain.model.Comment;
import com.cardapio.review.domain.model.Rating;
import com.cardapio.review.domain.model.Review;
import com.cardapio.review.domain.model.ReviewableOrder;
import com.cardapio.review.domain.port.ReviewRepository;
import com.cardapio.review.domain.port.ReviewableOrderRepository;
import com.cardapio.shared.domain.ErrorCode;
import com.cardapio.shared.domain.Result;
import com.cardapio.shared.metrics.DomainMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@RequiredArgsConstructor
public class SubmitReviewUseCase {

    private final ReviewableOrderRepository reviewableOrders;
    private final ReviewRepository reviews;
    private final DomainMetrics metrics;
    private final Clock clock;

    @Transactional
    public Result<ReviewView> execute(SubmitReviewCommand cmd) {
        ReviewableOrder reviewable = reviewableOrders.findByOrderId(cmd.orderId()).orElse(null);
        if (reviewable == null || !reviewable.customerId().equals(cmd.customerId())) {
            return Result.failWith(ErrorCode.ORDER_NOT_REVIEWABLE);
        }
        if (reviews.existsByOrderIdAndCustomerId(cmd.orderId(), cmd.customerId())) {
            return Result.failWith(ErrorCode.REVIEW_ALREADY_SUBMITTED);
        }

        Rating rating;
        try {
            rating = Rating.of(cmd.rating());
        } catch (IllegalArgumentException e) {
            return Result.failWith(ErrorCode.INVALID_RATING, e.getMessage());
        }

        Comment comment;
        try {
            comment = Comment.of(cmd.comment());
        } catch (IllegalArgumentException e) {
            return Result.failWith(ErrorCode.INVALID_COMMENT, e.getMessage());
        }

        Review review = Review.create(cmd.orderId(), cmd.customerId(), rating, comment, clock);
        try {
            reviews.save(review);
        } catch (DataIntegrityViolationException race) {
            return Result.failWith(ErrorCode.REVIEW_ALREADY_SUBMITTED);
        }
        metrics.countReviewSubmitted();
        return Result.success(ReviewView.from(review));
    }
}
