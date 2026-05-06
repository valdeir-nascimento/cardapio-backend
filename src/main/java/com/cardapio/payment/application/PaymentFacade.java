package com.cardapio.payment.application;

import com.cardapio.api.error.NotFoundException;
import com.cardapio.api.error.NotificationException;
import com.cardapio.payment.application.command.HandleWebhookCommand;
import com.cardapio.payment.application.command.InitiatePaymentCommand;
import com.cardapio.payment.application.command.RefundPaymentCommand;
import com.cardapio.payment.application.dto.InitiatedPaymentView;
import com.cardapio.payment.application.dto.PaymentView;
import com.cardapio.payment.domain.model.PaymentTransactionId;

import java.util.UUID;

public interface PaymentFacade {

    InitiatedPaymentView initiate(InitiatePaymentCommand cmd) throws NotificationException;

    /** Sempre retorna 200 (mesmo em assinatura inválida — log silencioso). */
    void handleWebhook(HandleWebhookCommand cmd);

    PaymentView getMyPayment(UUID customerId, PaymentTransactionId id) throws NotFoundException;

    PaymentView getPaymentAdmin(PaymentTransactionId id) throws NotFoundException;

    void refund(RefundPaymentCommand cmd) throws NotificationException;
}
