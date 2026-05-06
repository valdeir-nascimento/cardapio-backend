package com.cardapio.payment.domain.port;

import com.cardapio.payment.domain.model.CardCharge;
import com.cardapio.payment.domain.model.PaymentStatus;
import com.cardapio.payment.domain.model.PixCharge;
import com.cardapio.payment.domain.model.RefundResult;
import com.cardapio.shared.domain.Money;

import java.util.Map;
import java.util.UUID;

public interface PaymentGateway {

    PixCharge createPixCharge(Money amount, String orderRef, PayerInfo payer);

    CardCharge chargeCard(Money amount, String cardToken, String orderRef, PayerInfo payer);

    RefundResult refund(String gatewayTxId, Money amount);

    boolean verifyWebhookSignature(String rawBody, Map<String, String> headers);

    PaymentStatus fetchStatus(String gatewayTxId);

    record PayerInfo(UUID customerId, String email, String name, String taxId) {}
}
