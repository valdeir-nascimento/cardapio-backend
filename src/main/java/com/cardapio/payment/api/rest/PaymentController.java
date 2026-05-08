package com.cardapio.payment.api.rest;

import com.cardapio.identity.api.security.CardapioPrincipal;
import com.cardapio.payment.api.dto.InitiatePaymentRequest;
import com.cardapio.payment.api.dto.InitiatedPaymentResponse;
import com.cardapio.payment.api.dto.PaymentResponse;
import com.cardapio.payment.application.PaymentFacade;
import com.cardapio.payment.application.dto.InitiatedPaymentView;
import com.cardapio.payment.domain.model.PaymentTransactionId;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class PaymentController implements PaymentApi {

    private final PaymentFacade payments;

    @Override
    @PostMapping("/api/v1/orders/{orderId}/payment")
    public ResponseEntity<InitiatedPaymentResponse> initiate(
        @AuthenticationPrincipal CardapioPrincipal me,
        @PathVariable UUID orderId,
        InitiatePaymentRequest body
    ) {
        InitiatedPaymentView view = payments.initiate(body.toCommand(orderId, me.subject()));
        return ResponseEntity.created(URI.create("/api/v1/payments/" + view.id()))
            .body(InitiatedPaymentResponse.from(view));
    }

    @Override
    @GetMapping("/api/v1/payments/{id}")
    public PaymentResponse get(
        @AuthenticationPrincipal CardapioPrincipal me,
        @PathVariable UUID id
    ) {
        return PaymentResponse.from(payments.getMyPayment(me.subject(), PaymentTransactionId.of(id)));
    }
}
