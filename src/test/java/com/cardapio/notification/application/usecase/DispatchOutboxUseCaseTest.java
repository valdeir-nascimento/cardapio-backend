package com.cardapio.notification.application.usecase;

import com.cardapio.notification.domain.exception.NotificationDispatchException;
import com.cardapio.notification.domain.model.NotificationChannel;
import com.cardapio.notification.domain.model.NotificationOutbox;
import com.cardapio.notification.domain.model.NotificationStatus;
import com.cardapio.notification.domain.model.NotificationTemplate;
import com.cardapio.notification.domain.port.EmailSender;
import com.cardapio.notification.domain.port.NotificationOutboxRepository;
import com.cardapio.notification.domain.port.TemplateRenderer;
import com.cardapio.notification.domain.port.WhatsAppSender;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DispatchOutboxUseCaseTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-05-06T12:00:00Z"), ZoneOffset.UTC);
    private NotificationOutboxRepository repo;
    private TemplateRenderer renderer;
    private EmailSender email;
    private WhatsAppSender whats;
    private DispatchOutboxUseCase uc;

    @BeforeEach
    void setUp() {
        repo = mock(NotificationOutboxRepository.class);
        renderer = mock(TemplateRenderer.class);
        email = mock(EmailSender.class);
        whats = mock(WhatsAppSender.class);
        ObjectProvider<EmailSender> emailProvider = stubProvider(email);
        ObjectProvider<WhatsAppSender> whatsProvider = stubProvider(whats);
        PlatformTransactionManager txm = inlineTxManager();
        uc = new DispatchOutboxUseCase(repo, renderer, emailProvider, whatsProvider, new ObjectMapper(), clock, txm);
    }

    @Test
    void dispatchesEmailAndMarksSent() {
        NotificationOutbox box = enqueue(NotificationChannel.EMAIL,
            "{\"to\":\"a@b.com\",\"model\":{\"customer\":{\"name\":\"M\"}}}");
        when(repo.findDueForDispatch(any(), anyInt())).thenReturn(List.of(box));
        when(repo.findById(any())).thenReturn(Optional.of(box));
        when(renderer.renderEmail(eq(NotificationTemplate.ORDER_RECEIVED), any()))
            .thenReturn(new TemplateRenderer.EmailContent("Subj", "<p/>", null));

        int processed = uc.run();

        assertThat(processed).isEqualTo(1);
        verify(email).send("a@b.com", "Subj", "<p/>", null);
        ArgumentCaptor<NotificationOutbox> saved = ArgumentCaptor.forClass(NotificationOutbox.class);
        verify(repo).save(saved.capture());
        assertThat(saved.getValue().status()).isEqualTo(NotificationStatus.SENT);
    }

    @Test
    void dispatchesWhatsApp() {
        NotificationOutbox box = enqueue(NotificationChannel.WHATSAPP,
            "{\"to\":\"+5511999998888\",\"model\":{\"order\":{\"shortRef\":\"ABCDEF\"}}}");
        when(repo.findDueForDispatch(any(), anyInt())).thenReturn(List.of(box));
        when(repo.findById(any())).thenReturn(Optional.of(box));
        when(renderer.renderWhatsApp(eq(NotificationTemplate.ORDER_RECEIVED), any())).thenReturn("oi");

        uc.run();

        verify(whats).sendText("+5511999998888", "oi");
        verify(email, never()).send(any(), any(), any(), any());
    }

    @Test
    void onSenderFailureMarksFailedAndReschedules() {
        NotificationOutbox box = enqueue(NotificationChannel.EMAIL,
            "{\"to\":\"a@b.com\",\"model\":{}}");
        when(repo.findDueForDispatch(any(), anyInt())).thenReturn(List.of(box));
        when(repo.findById(any())).thenReturn(Optional.of(box));
        when(renderer.renderEmail(any(), any()))
            .thenReturn(new TemplateRenderer.EmailContent("S", "<p/>", null));
        doThrow(new NotificationDispatchException("RESEND_5XX", "boom"))
            .when(email).send(any(), any(), any(), any());

        uc.run();

        ArgumentCaptor<NotificationOutbox> saved = ArgumentCaptor.forClass(NotificationOutbox.class);
        verify(repo).save(saved.capture());
        assertThat(saved.getValue().status()).isEqualTo(NotificationStatus.FAILED);
        assertThat(saved.getValue().attempts()).isEqualTo(1);
        assertThat(saved.getValue().lastError()).contains("boom");
    }

    private NotificationOutbox enqueue(NotificationChannel channel, String payload) {
        return NotificationOutbox.rehydrate(
            com.cardapio.notification.domain.model.NotificationOutboxId.newId(),
            channel,
            NotificationTemplate.ORDER_RECEIVED,
            UUID.randomUUID(),
            payload,
            NotificationStatus.PENDING,
            0, null,
            clock.instant(),
            clock.instant(),
            clock.instant());
    }

    private static <T> ObjectProvider<T> stubProvider(T bean) {
        @SuppressWarnings("unchecked")
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(bean);
        return provider;
    }

    private static PlatformTransactionManager inlineTxManager() {
        return new PlatformTransactionManager() {
            @Override
            public TransactionStatus getTransaction(org.springframework.transaction.TransactionDefinition definition) {
                return new SimpleTransactionStatus(true);
            }
            @Override public void commit(TransactionStatus status) {}
            @Override public void rollback(TransactionStatus status) {}
        };
    }

    @Test
    void abandonsAfterFiveFailures() {
        Map<String, NotificationOutbox> store = new HashMap<>();
        NotificationOutbox box = enqueue(NotificationChannel.EMAIL, "{\"to\":\"a@b.com\",\"model\":{}}");
        store.put(box.id().value().toString(), box);
        when(repo.findDueForDispatch(any(), anyInt())).thenAnswer(inv -> List.copyOf(store.values()));
        when(repo.findById(any())).thenAnswer(inv ->
            Optional.ofNullable(store.get(((com.cardapio.notification.domain.model.NotificationOutboxId) inv.getArgument(0)).value().toString())));
        when(renderer.renderEmail(any(), any()))
            .thenReturn(new TemplateRenderer.EmailContent("S", "<p/>", null));
        doThrow(new NotificationDispatchException("RESEND_5XX", "boom"))
            .when(email).send(any(), any(), any(), any());

        for (int i = 0; i < 5; i++) {
            // simulate scheduledFor reached by re-rehydrating with PENDING-due
            NotificationOutbox stored = store.values().iterator().next();
            store.put(stored.id().value().toString(), NotificationOutbox.rehydrate(
                stored.id(), stored.channel(), stored.template(), stored.recipientId(),
                stored.payload(), NotificationStatus.PENDING, stored.attempts(),
                stored.lastError(), clock.instant(), stored.createdAt(), stored.updatedAt()));
            uc.run();
        }

        NotificationOutbox finalBox = store.values().iterator().next();
        assertThat(finalBox.attempts()).isEqualTo(NotificationOutbox.MAX_ATTEMPTS);
        assertThat(finalBox.status()).isEqualTo(NotificationStatus.ABANDONED);
    }
}
