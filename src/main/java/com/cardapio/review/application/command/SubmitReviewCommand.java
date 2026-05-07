package com.cardapio.review.application.command;

import com.cardapio.ordering.domain.model.OrderId;

import java.util.UUID;

public record SubmitReviewCommand(OrderId orderId, UUID customerId, int rating, String comment) {}
