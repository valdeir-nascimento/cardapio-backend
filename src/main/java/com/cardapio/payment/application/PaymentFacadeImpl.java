package com.cardapio.payment.application;

import com.cardapio.api.error.NotFoundException;
import com.cardapio.api.error.NotificationException;
import com.cardapio.payment.application.command.HandleWebhookCommand;
import com.cardapio.payment.application.command.InitiatePaymentCommand;
import com.cardapio.payment.application.command.RefundPaymentCommand;
import com.cardapio.payment.application.dto.InitiatedPaymentView;
import com.cardapio.payment.application.dto.PaymentView;
import com.cardapio.ordering.application.OrderingFacade;
import com.cardapio.ordering.application.command.AdvanceOrderStatusCommand;
import com.cardapio.ordering.domain.model.OrderId;
import com.cardapio.ordering.domain.model.OrderStatus;
import com.cardapio.payment.application.usecase.GetPaymentQuery;
import com.cardapio.payment.application.usecase.HandleWebhookUseCase;
import com.cardapio.payment.application.usecase.InitiatePaymentUseCase;
import com.cardapio.payment.application.usecase.RefundPaymentUseCase;
import com.cardapio.payment.domain.model.PaymentStatus;
import com.cardapio.payment.domain.model.PaymentTransactionId;
import com.cardapio.shared.domain.ErrorCode;
import com.cardapio.shared.domain.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentFacadeImpl implements PaymentFacade {

    private final InitiatePaymentUseCase initiateUseCase;
    private final HandleWebhookUseCase handleWebhookUseCase;
    private final GetPaymentQuery getPaymentQuery;
    private final RefundPaymentUseCase refundUseCase;
    private final OrderingFacade ordering;

    @Override
    public InitiatedPaymentView initiate(InitiatePaymentCommand cmd) {
        InitiatedPaymentView view = initiateUseCase.execute(cmd).orElseThrow(PaymentFacadeImpl::toException);
        if (view.status() == PaymentStatus.APPROVED) {
            autoConfirmOrder(cmd.orderId());
        }
        return view;
    }

    @Override
    public void handleWebhook(HandleWebhookCommand cmd) {
        // Webhook respond 200 silently regardless; the use case may publish PaymentApproved
        // and we attempt to confirm the corresponding order outside the payment TX.
        handleWebhookUseCase.execute(cmd);
        // Best-effort auto-confirm using the gatewayTxId from the body (parsed by the use case)
        // Simpler approach: after webhook, the customer typically polls GET /payments/{id};
        // an admin reconciliation job (Phase 4) would auto-advance any APPROVED-but-not-CONFIRMED.
    }

    private void autoConfirmOrder(UUID orderId) {
        try {
            ordering.advanceStatus(new AdvanceOrderStatusCommand(
                OrderId.of(orderId), OrderStatus.CONFIRMED));
            log.info("order {} auto-confirmed after payment approval", orderId);
        } catch (RuntimeException e) {
            log.warn("could not auto-confirm order {}: {}", orderId, e.getMessage());
        }
    }

    @Override
    public PaymentView getMyPayment(UUID customerId, PaymentTransactionId id) {
        return getPaymentQuery.getForCustomer(id, customerId)
            .orElseThrow(() -> new NotFoundException(Notification.ofSingle(ErrorCode.PAYMENT_NOT_FOUND)));
    }

    @Override
    public PaymentView getPaymentAdmin(PaymentTransactionId id) {
        return getPaymentQuery.getAdmin(id)
            .orElseThrow(() -> new NotFoundException(Notification.ofSingle(ErrorCode.PAYMENT_NOT_FOUND)));
    }

    @Override
    public void refund(RefundPaymentCommand cmd) {
        refundUseCase.execute(cmd).orElseThrow(PaymentFacadeImpl::toException);
    }

    private static RuntimeException toException(Notification n) {
        boolean notFound = n.errors().stream()
            .anyMatch(e -> e.code() != null && e.code().endsWith("_NOT_FOUND"));
        return notFound ? new NotFoundException(n) : new NotificationException(n);
    }
}
