package com.cardapio.payment.application.usecase;

import com.cardapio.payment.application.command.RefundPaymentCommand;
import com.cardapio.payment.domain.event.PaymentRefunded;
import com.cardapio.payment.domain.exception.PaymentGatewayException;
import com.cardapio.payment.domain.model.PaymentStatus;
import com.cardapio.payment.domain.model.PaymentTransaction;
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
public class RefundPaymentUseCase {

    private final PaymentTransactionRepository repo;
    private final PaymentGateway gateway;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Transactional
    public Result<Void> execute(RefundPaymentCommand cmd) {
        PaymentTransaction tx = repo.findById(cmd.paymentId()).orElse(null);
        if (tx == null) return Result.failWith(ErrorCode.PAYMENT_NOT_FOUND);
        if (tx.status() != PaymentStatus.APPROVED) {
            return Result.failWith(ErrorCode.PAYMENT_NOT_REFUNDABLE);
        }

        try {
            gateway.refund(tx.gatewayTxId(), tx.amount());
        } catch (PaymentGatewayException e) {
            log.warn("refund failed for payment {}: {}", tx.id().value(), e.getMessage());
            return Result.failWith(ErrorCode.PAYMENT_GATEWAY_FAILED);
        }

        tx.markRefunded(clock);
        repo.save(tx);
        events.publishEvent(PaymentRefunded.of(tx, clock.instant()));
        return Result.ok();
    }
}
