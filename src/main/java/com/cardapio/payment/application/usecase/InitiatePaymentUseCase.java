package com.cardapio.payment.application.usecase;

import com.cardapio.ordering.domain.model.OrderStatus;
import com.cardapio.payment.application.command.InitiatePaymentCommand;
import com.cardapio.payment.application.dto.InitiatedPaymentView;
import com.cardapio.payment.domain.event.PaymentApproved;
import com.cardapio.payment.domain.event.PaymentRejected;
import com.cardapio.payment.domain.exception.PaymentGatewayException;
import com.cardapio.payment.domain.model.CardCharge;
import com.cardapio.payment.domain.model.PaymentMethod;
import com.cardapio.payment.domain.model.PaymentStatus;
import com.cardapio.payment.domain.model.PaymentTransaction;
import com.cardapio.payment.domain.model.PixCharge;
import com.cardapio.payment.domain.port.OrderQueryPort;
import com.cardapio.payment.domain.port.PaymentGateway;
import com.cardapio.payment.domain.port.PaymentTransactionRepository;
import com.cardapio.shared.domain.ErrorCode;
import com.cardapio.shared.domain.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@RequiredArgsConstructor
@Slf4j
public class InitiatePaymentUseCase {

    private final PaymentTransactionRepository repo;
    private final PaymentGateway gateway;
    private final OrderQueryPort orders;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Transactional
    public Result<InitiatedPaymentView> execute(InitiatePaymentCommand cmd) {
        var snapshot = orders.loadOrder(cmd.orderId()).orElse(null);
        if (snapshot == null) return Result.failWith(ErrorCode.ORDER_NOT_FOUND);
        if (!snapshot.customerId().equals(cmd.customerId())) {
            return Result.failWith(ErrorCode.ORDER_NOT_FOUND);
        }
        if (snapshot.status() != OrderStatus.RECEIVED) {
            return Result.failWith(ErrorCode.PAYMENT_ORDER_NOT_PAYABLE);
        }
        if (repo.findActiveByOrderId(cmd.orderId()).isPresent()) {
            return Result.failWith(ErrorCode.PAYMENT_ALREADY_INITIATED);
        }
        if (cmd.method() == PaymentMethod.CARD && (cmd.cardToken() == null || cmd.cardToken().isBlank())) {
            return Result.failWith(ErrorCode.CARD_TOKEN_REQUIRED);
        }

        var tx = PaymentTransaction.initiate(snapshot.orderId(), snapshot.customerId(),
            cmd.method(), snapshot.total(), clock);

        var payer = new PaymentGateway.PayerInfo(snapshot.customerId(), null, null, null);
        String orderRef = tx.id().value().toString();

        try {
            switch (cmd.method()) {
                case PIX -> {
                    PixCharge pix = gateway.createPixCharge(snapshot.total(), orderRef, payer);
                    tx.attachPixCharge(pix, clock);
                }
                case CARD -> {
                    CardCharge charge = gateway.chargeCard(snapshot.total(), cmd.cardToken(), orderRef, payer);
                    tx.attachCardCharge(charge, clock);
                    if (charge.status() == PaymentStatus.APPROVED) {
                        events.publishEvent(PaymentApproved.of(tx, clock.instant()));
                    } else if (charge.status() == PaymentStatus.REJECTED) {
                        events.publishEvent(PaymentRejected.of(tx, clock.instant()));
                    }
                }
            }
        } catch (PaymentGatewayException e) {
            log.warn("payment gateway failed for order {}: {}", cmd.orderId(), e.getMessage());
            tx.markFailed(clock, e.getMessage());
            repo.save(tx);
            return Result.failWith(ErrorCode.PAYMENT_GATEWAY_FAILED);
        }

        repo.save(tx);
        return Result.success(InitiatedPaymentView.from(tx));
    }
}
