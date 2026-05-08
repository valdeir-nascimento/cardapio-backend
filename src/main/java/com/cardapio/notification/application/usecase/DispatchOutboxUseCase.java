package com.cardapio.notification.application.usecase;

import com.cardapio.notification.domain.model.NotificationChannel;
import com.cardapio.notification.domain.model.NotificationOutbox;
import com.cardapio.notification.domain.model.NotificationTemplate;
import com.cardapio.notification.domain.port.EmailSender;
import com.cardapio.notification.domain.port.NotificationOutboxRepository;
import com.cardapio.notification.domain.port.TemplateRenderer;
import com.cardapio.notification.domain.port.WhatsAppSender;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class DispatchOutboxUseCase {

    private static final int BATCH_SIZE = 50;

    private final NotificationOutboxRepository repository;
    private final TemplateRenderer renderer;
    private final ObjectProvider<EmailSender> emailSender;
    private final ObjectProvider<WhatsAppSender> whatsAppSender;
    private final ObjectMapper mapper;
    private final Clock clock;
    private final TransactionTemplate tx;

    public DispatchOutboxUseCase(NotificationOutboxRepository repository,
                                 TemplateRenderer renderer,
                                 ObjectProvider<EmailSender> emailSender,
                                 ObjectProvider<WhatsAppSender> whatsAppSender,
                                 ObjectMapper mapper,
                                 Clock clock,
                                 PlatformTransactionManager txManager) {
        this.repository = repository;
        this.renderer = renderer;
        this.emailSender = emailSender;
        this.whatsAppSender = whatsAppSender;
        this.mapper = mapper;
        this.clock = clock;
        this.tx = new TransactionTemplate(txManager);
        this.tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public int run() {
        List<NotificationOutbox> due = repository.findDueForDispatch(clock.instant(), BATCH_SIZE);
        int processed = 0;
        for (NotificationOutbox box : due) {
            try {
                tx.executeWithoutResult(s -> dispatchOne(box.id().value()));
                processed++;
            } catch (Exception e) {
                log.error("dispatch tx failed id={}", box.id().value(), e);
            }
        }
        return processed;
    }

    private void dispatchOne(java.util.UUID id) {
        NotificationOutbox box = repository.findById(
            com.cardapio.notification.domain.model.NotificationOutboxId.of(id)).orElse(null);
        if (box == null || box.status().isTerminal()) return;
        if (!box.isDue(clock.instant())) return;

        try {
            Payload payload = parse(box.payload());
            switch (box.channel()) {
                case EMAIL -> sendEmail(box.template(), payload);
                case WHATSAPP -> sendWhatsApp(box.template(), payload);
                case SSE_ADMIN, SSE_CUSTOMER ->
                    throw new IllegalStateException("SSE channels must not be persisted");
            }
            box.markSent(clock);
        } catch (Exception e) {
            log.warn("dispatch failed id={} channel={} attempts={} error={}",
                box.id().value(), box.channel(), box.attempts() + 1, e.getMessage());
            box.markFailed(truncate(e.getMessage()), clock);
        }
        repository.save(box);
    }

    private void sendEmail(NotificationTemplate template, Payload payload) {
        EmailSender sender = emailSender.getIfAvailable();
        if (sender == null) {
            throw new IllegalStateException("EmailSender not configured (resend.enabled=false)");
        }
        TemplateRenderer.EmailContent content = renderer.renderEmail(template, payload.model());
        sender.send(payload.to(), content.subject(), content.html(), content.text());
    }

    private void sendWhatsApp(NotificationTemplate template, Payload payload) {
        WhatsAppSender sender = whatsAppSender.getIfAvailable();
        if (sender == null) {
            throw new IllegalStateException("WhatsAppSender not configured (wppconnect.enabled=false)");
        }
        String body = renderer.renderWhatsApp(template, payload.model());
        sender.sendText(payload.to(), body);
    }

    private Payload parse(String json) {
        try {
            Map<String, Object> raw = mapper.readValue(json, new TypeReference<>() {});
            String to = (String) raw.get("to");
            @SuppressWarnings("unchecked")
            Map<String, Object> model = (Map<String, Object>) raw.get("model");
            return new Payload(to, model == null ? Map.of() : model);
        } catch (Exception e) {
            throw new IllegalStateException("malformed notification payload", e);
        }
    }

    private static String truncate(String s) {
        if (s == null) return null;
        return s.length() > 1000 ? s.substring(0, 1000) : s;
    }

    private record Payload(String to, Map<String, Object> model) {}

    /** Pulled out so listeners can call NotificationChannel name resolution. */
    public static String channelToProvider(NotificationChannel c) {
        return switch (c) {
            case EMAIL -> "resend";
            case WHATSAPP -> "wppconnect";
            default -> "n/a";
        };
    }
}
