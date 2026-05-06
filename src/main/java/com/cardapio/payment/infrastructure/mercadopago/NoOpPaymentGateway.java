package com.cardapio.payment.infrastructure.mercadopago;

import com.cardapio.payment.domain.exception.PaymentGatewayException;
import com.cardapio.payment.domain.model.CardCharge;
import com.cardapio.payment.domain.model.PaymentStatus;
import com.cardapio.payment.domain.model.PixCharge;
import com.cardapio.payment.domain.model.RefundResult;
import com.cardapio.payment.domain.port.PaymentGateway;
import com.cardapio.shared.domain.Money;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Fallback gateway used when {@code mercadopago.enabled=false} (default in tests).
 * Tests should override with {@code @MockBean PaymentGateway} when exercising payment use cases.
 */
@Component
@ConditionalOnProperty(name = "mercadopago.enabled", havingValue = "false", matchIfMissing = true)
@Slf4j
class NoOpPaymentGateway implements PaymentGateway {

    @Override
    public PixCharge createPixCharge(Money amount, String orderRef, PayerInfo payer) {
        throw notConfigured();
    }

    @Override
    public CardCharge chargeCard(Money amount, String cardToken, String orderRef, PayerInfo payer) {
        throw notConfigured();
    }

    @Override
    public RefundResult refund(String gatewayTxId, Money amount) {
        throw notConfigured();
    }

    @Override
    public boolean verifyWebhookSignature(String rawBody, Map<String, String> headers) {
        log.warn("MP disabled — webhook signature treated as invalid");
        return false;
    }

    @Override
    public PaymentStatus fetchStatus(String gatewayTxId) {
        throw notConfigured();
    }

    private static PaymentGatewayException notConfigured() {
        return new PaymentGatewayException("MP_DISABLED",
            "Mercado Pago integration is disabled (set mercadopago.enabled=true)");
    }
}
