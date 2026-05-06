package com.cardapio.payment.domain.model;

import com.cardapio.shared.domain.Money;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentTransactionTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-05-05T12:00:00Z"), ZoneOffset.UTC);
    private final UUID orderId = UUID.randomUUID();
    private final UUID customerId = UUID.randomUUID();
    private final Money amount = Money.brl("100.00");

    @Test
    void initiatesAsPending() {
        PaymentTransaction tx = PaymentTransaction.initiate(orderId, customerId, PaymentMethod.PIX, amount, clock);
        assertThat(tx.status()).isEqualTo(PaymentStatus.PENDING);
        assertThat(tx.orderId()).isEqualTo(orderId);
        assertThat(tx.gatewayTxId()).isNull();
    }

    @Test
    void attachPixOnlyOnPending() {
        PaymentTransaction tx = PaymentTransaction.initiate(orderId, customerId, PaymentMethod.PIX, amount, clock);
        PixCharge pix = new PixCharge("mp-1", "qr-payload", "qr-base64", null);
        tx.attachPixCharge(pix, clock);
        assertThat(tx.gatewayTxId()).isEqualTo("mp-1");
        assertThat(tx.qrCode()).isEqualTo("qr-payload");

        tx.approve(clock);
        assertThatThrownBy(() -> tx.attachPixCharge(pix, clock))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void attachPixOnCardTransactionThrows() {
        PaymentTransaction tx = PaymentTransaction.initiate(orderId, customerId, PaymentMethod.CARD, amount, clock);
        PixCharge pix = new PixCharge("mp-1", "qr", "qr64", null);
        assertThatThrownBy(() -> tx.attachPixCharge(pix, clock))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cardChargeMaySetTerminalImmediately() {
        PaymentTransaction tx = PaymentTransaction.initiate(orderId, customerId, PaymentMethod.CARD, amount, clock);
        CardCharge charge = new CardCharge("mp-2", PaymentStatus.REJECTED, "1234", "VISA", null);
        tx.attachCardCharge(charge, clock);
        assertThat(tx.status()).isEqualTo(PaymentStatus.REJECTED);
        assertThat(tx.cardLast4()).isEqualTo("1234");
        assertThat(tx.cardBrand()).isEqualTo("VISA");
    }

    @Test
    void approveTransitionsFromPending() {
        PaymentTransaction tx = PaymentTransaction.initiate(orderId, customerId, PaymentMethod.PIX, amount, clock);
        tx.approve(clock);
        assertThat(tx.status()).isEqualTo(PaymentStatus.APPROVED);
    }

    @Test
    void approveRejectsWhenAlreadyTerminalWithDifferentStatus() {
        PaymentTransaction tx = PaymentTransaction.initiate(orderId, customerId, PaymentMethod.PIX, amount, clock);
        tx.reject(clock);
        assertThatThrownBy(() -> tx.approve(clock)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void approveIsIdempotent() {
        PaymentTransaction tx = PaymentTransaction.initiate(orderId, customerId, PaymentMethod.PIX, amount, clock);
        tx.approve(clock);
        tx.approve(clock);
        assertThat(tx.status()).isEqualTo(PaymentStatus.APPROVED);
    }

    @Test
    void refundOnlyFromApproved() {
        PaymentTransaction tx = PaymentTransaction.initiate(orderId, customerId, PaymentMethod.PIX, amount, clock);
        assertThatThrownBy(() -> tx.markRefunded(clock)).isInstanceOf(IllegalStateException.class);
        tx.approve(clock);
        tx.markRefunded(clock);
        assertThat(tx.status()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    void recordWebhookEventAppendsToLog() {
        PaymentTransaction tx = PaymentTransaction.initiate(orderId, customerId, PaymentMethod.PIX, amount, clock);
        tx.recordWebhookEvent(new WebhookEventLog(UUID.randomUUID(), "hash1", "{}", clock.instant()));
        tx.recordWebhookEvent(new WebhookEventLog(UUID.randomUUID(), "hash2", "{}", clock.instant()));
        assertThat(tx.events()).hasSize(2);
    }

    @Test
    void markFailedOnlyFromNonTerminal() {
        PaymentTransaction tx = PaymentTransaction.initiate(orderId, customerId, PaymentMethod.PIX, amount, clock);
        tx.markFailed(clock, "MP unavailable");
        assertThat(tx.status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(tx.failureReason()).isEqualTo("MP unavailable");
    }
}
