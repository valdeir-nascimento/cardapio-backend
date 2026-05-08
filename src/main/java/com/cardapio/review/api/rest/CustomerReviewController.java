package com.cardapio.review.api.rest;

import com.cardapio.api.error.NotFoundException;
import com.cardapio.api.error.NotificationException;
import com.cardapio.identity.api.security.CardapioPrincipal;
import com.cardapio.ordering.domain.model.OrderId;
import com.cardapio.review.api.dto.ReviewResponse;
import com.cardapio.review.api.dto.ReviewableOrderResponse;
import com.cardapio.review.api.dto.SubmitReviewRequest;
import com.cardapio.review.application.command.SubmitReviewCommand;
import com.cardapio.review.application.usecase.GetMyReviewUseCase;
import com.cardapio.review.application.usecase.GetMyReviewableOrdersUseCase;
import com.cardapio.review.application.usecase.SubmitReviewUseCase;
import com.cardapio.shared.domain.Notification;
import com.cardapio.shared.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerReviewController implements CustomerReviewApi {

    private final SubmitReviewUseCase submitReview;
    private final GetMyReviewUseCase getMyReview;
    private final GetMyReviewableOrdersUseCase getMyReviewableOrders;

    @Override
    @GetMapping("/api/v1/me/reviewable-orders")
    public List<ReviewableOrderResponse> reviewable(
        @AuthenticationPrincipal CardapioPrincipal me,
        @RequestParam(defaultValue = "20") int limit,
        @RequestParam(defaultValue = "0") int offset
    ) {
        return getMyReviewableOrders.execute(me.subject(), limit, offset).stream()
            .map(ReviewableOrderResponse::from).toList();
    }

    @Override
    @PostMapping("/api/v1/orders/{id}/review")
    public ResponseEntity<ReviewResponse> submit(
        @AuthenticationPrincipal CardapioPrincipal me,
        @PathVariable UUID id,
        SubmitReviewRequest body
    ) {
        var view = unwrap(submitReview.execute(new SubmitReviewCommand(
            OrderId.of(id), me.subject(), body.rating(), body.comment())));
        return ResponseEntity
            .created(URI.create("/api/v1/orders/" + id + "/review"))
            .body(ReviewResponse.from(view));
    }

    @Override
    @GetMapping("/api/v1/orders/{id}/review")
    public ReviewResponse getMine(
        @AuthenticationPrincipal CardapioPrincipal me,
        @PathVariable UUID id
    ) {
        return ReviewResponse.from(unwrap(getMyReview.execute(OrderId.of(id), me.subject())));
    }

    private static <T> T unwrap(Result<T> r) {
        return r.orElseThrow(CustomerReviewController::toException);
    }

    private static RuntimeException toException(Notification n) {
        boolean notFound = n.errors().stream()
            .anyMatch(e -> e.code() != null && e.code().endsWith("_NOT_FOUND"));
        return notFound ? new NotFoundException(n) : new NotificationException(n);
    }
}
