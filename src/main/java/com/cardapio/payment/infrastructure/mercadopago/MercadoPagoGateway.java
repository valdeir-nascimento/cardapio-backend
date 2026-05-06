package com.cardapio.payment.infrastructure.mercadopago;

import com.cardapio.payment.domain.exception.PaymentGatewayException;
import com.cardapio.payment.domain.model.CardCharge;
import com.cardapio.payment.domain.model.PaymentStatus;
import com.cardapio.payment.domain.model.PixCharge;
import com.cardapio.payment.domain.model.RefundResult;
import com.cardapio.payment.domain.port.PaymentGateway;
import com.cardapio.shared.domain.Money;
import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.client.payment.PaymentCreateRequest;
import com.mercadopago.client.payment.PaymentPayerRequest;
import com.mercadopago.client.payment.PaymentRefundClient;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.payment.Payment;
import com.mercadopago.resources.payment.PaymentRefund;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConditionalOnProperty(name = "mercadopago.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
class MercadoPagoGateway implements PaymentGateway {

    private final MercadoPagoProperties props;
    private final MercadoPagoSignatureVerifier signatureVerifier;
    private final PaymentClient paymentClient = new PaymentClient();
    private final PaymentRefundClient refundClient = new PaymentRefundClient();

    @Retry(name = "mercadopago")
    @CircuitBreaker(name = "mercadopago")
    @Override
    public PixCharge createPixCharge(Money amount, String orderRef, PayerInfo payer) {
        PaymentCreateRequest request = PaymentCreateRequest.builder()
            .transactionAmount(amount.amount())
            .description("Pedido " + orderRef)
            .paymentMethodId("pix")
            .externalReference(orderRef)
            .notificationUrl(props.notificationUrl())
            .payer(PaymentPayerRequest.builder()
                .email(payer.email() != null ? payer.email() : "customer@example.com")
                .firstName(payer.name() != null ? payer.name() : "Cliente")
                .build())
            .build();
        try {
            Payment payment = paymentClient.create(request);
            var poi = payment.getPointOfInteraction().getTransactionData();
            return new PixCharge(
                String.valueOf(payment.getId()),
                poi.getQrCode(),
                poi.getQrCodeBase64(),
                payment.getDateOfExpiration() != null ? payment.getDateOfExpiration().toInstant() : null
            );
        } catch (MPApiException | MPException e) {
            throw new PaymentGatewayException("MP_PIX_FAILED", e.getMessage(), e);
        }
    }

    @Retry(name = "mercadopago")
    @CircuitBreaker(name = "mercadopago")
    @Override
    public CardCharge chargeCard(Money amount, String cardToken, String orderRef, PayerInfo payer) {
        PaymentCreateRequest request = PaymentCreateRequest.builder()
            .transactionAmount(amount.amount())
            .token(cardToken)
            .description("Pedido " + orderRef)
            .externalReference(orderRef)
            .installments(1)
            .notificationUrl(props.notificationUrl())
            .payer(PaymentPayerRequest.builder()
                .email(payer.email() != null ? payer.email() : "customer@example.com")
                .build())
            .build();
        try {
            Payment payment = paymentClient.create(request);
            PaymentStatus status = mapStatus(payment.getStatus());
            String last4 = payment.getCard() != null ? payment.getCard().getLastFourDigits() : null;
            String brand = payment.getPaymentMethodId();
            return new CardCharge(String.valueOf(payment.getId()), status, last4, brand,
                String.valueOf(payment.getAuthorizationCode()));
        } catch (MPApiException | MPException e) {
            throw new PaymentGatewayException("MP_CARD_FAILED", e.getMessage(), e);
        }
    }

    @Retry(name = "mercadopago")
    @Override
    public RefundResult refund(String gatewayTxId, Money amount) {
        try {
            PaymentRefund refund = refundClient.refund(Long.parseLong(gatewayTxId));
            return new RefundResult(
                String.valueOf(refund.getId()),
                refund.getDateCreated().toInstant()
            );
        } catch (MPApiException | MPException e) {
            throw new PaymentGatewayException("MP_REFUND_FAILED", e.getMessage(), e);
        }
    }

    @Override
    public boolean verifyWebhookSignature(String rawBody, Map<String, String> headers) {
        return signatureVerifier.verify(rawBody, headers);
    }

    @Retry(name = "mercadopago")
    @CircuitBreaker(name = "mercadopago")
    @Override
    public PaymentStatus fetchStatus(String gatewayTxId) {
        try {
            Payment payment = paymentClient.get(Long.parseLong(gatewayTxId));
            return mapStatus(payment.getStatus());
        } catch (MPApiException | MPException e) {
            throw new PaymentGatewayException("MP_FETCH_STATUS_FAILED", e.getMessage(), e);
        }
    }

    private static PaymentStatus mapStatus(String mpStatus) {
        if (mpStatus == null) return PaymentStatus.PENDING;
        return switch (mpStatus) {
            case "approved" -> PaymentStatus.APPROVED;
            case "rejected", "cancelled" -> PaymentStatus.REJECTED;
            case "refunded" -> PaymentStatus.REFUNDED;
            case "pending", "in_process", "authorized" -> PaymentStatus.PENDING;
            default -> {
                log.warn("unknown MP status: {}", mpStatus);
                yield PaymentStatus.PENDING;
            }
        };
    }
}
