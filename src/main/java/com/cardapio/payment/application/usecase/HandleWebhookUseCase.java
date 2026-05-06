package com.cardapio.payment.application.usecase;

import com.cardapio.payment.application.command.HandleWebhookCommand;
import com.cardapio.payment.domain.event.PaymentApproved;
import com.cardapio.payment.domain.event.PaymentRejected;
import com.cardapio.payment.domain.model.PaymentStatus;
import com.cardapio.payment.domain.model.PaymentTransaction;
import com.cardapio.payment.domain.model.WebhookEventLog;
import com.cardapio.payment.domain.port.PaymentGateway;
import com.cardapio.payment.domain.port.PaymentTransactionRepository;
import com.cardapio.payment.domain.port.ProcessedWebhookStore;
import com.cardapio.payment.domain.port.WebhookPayloadParser;
import com.cardapio.shared.domain.ErrorCode;
import com.cardapio.shared.domain.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class HandleWebhookUseCase {

    private final PaymentGateway gateway;
    private final PaymentTransactionRepository repo;
    private final ProcessedWebhookStore processed;
    private final WebhookPayloadParser parser;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Transactional
    public Result<Void> execute(HandleWebhookCommand cmd) {
        if (!gateway.verifyWebhookSignature(cmd.rawBody(), cmd.headers())) {
            log.warn("invalid webhook signature");
            return Result.failWith(ErrorCode.PAYMENT_INVALID_WEBHOOK_SIGNATURE);
        }

        String hash = sha256(cmd.rawBody());
        if (!processed.registerIfAbsent(hash, clock.instant())) {
            log.debug("webhook already processed: {}", hash);
            return Result.ok();
        }

        WebhookPayloadParser.Parsed parsed = parser.parse(cmd.rawBody());
        if (!"payment".equals(parsed.type())) {
            log.debug("ignoring webhook with type={}", parsed.type());
            return Result.ok();
        }
        if (parsed.dataId() == null) {
            log.debug("webhook without data.id");
            return Result.ok();
        }

        PaymentTransaction tx = repo.findByGatewayTxId(parsed.dataId()).orElse(null);
        if (tx == null) {
            log.info("webhook for unknown gatewayTxId={} (race condition); will reconcile via GET", parsed.dataId());
            return Result.ok();
        }

        tx.recordWebhookEvent(new WebhookEventLog(
            UUID.randomUUID(), hash, cmd.rawBody(), clock.instant()));

        PaymentStatus current = gateway.fetchStatus(tx.gatewayTxId());

        if (current == PaymentStatus.APPROVED && tx.status() != PaymentStatus.APPROVED) {
            tx.approve(clock);
            events.publishEvent(PaymentApproved.of(tx, clock.instant()));
        } else if (current == PaymentStatus.REJECTED && tx.status() != PaymentStatus.REJECTED) {
            tx.reject(clock);
            events.publishEvent(PaymentRejected.of(tx, clock.instant()));
        }

        repo.save(tx);
        return Result.ok();
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
