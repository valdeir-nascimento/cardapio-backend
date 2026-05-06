package com.cardapio.notification.application.usecase;

import com.cardapio.notification.application.command.EnqueueNotificationCommand;
import com.cardapio.notification.domain.model.NotificationOutbox;
import com.cardapio.notification.domain.port.NotificationOutboxRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EnqueueNotificationUseCase {

    private final NotificationOutboxRepository repository;
    private final ObjectMapper mapper;
    private final Clock clock;

    @Transactional
    public void execute(EnqueueNotificationCommand cmd) {
        String payload = serialize(cmd.to(), cmd.model());
        NotificationOutbox box = NotificationOutbox.enqueue(
            cmd.channel(), cmd.template(), cmd.recipientId(), payload, clock);
        repository.save(box);
    }

    private String serialize(String to, Map<String, Object> model) {
        try {
            return mapper.writeValueAsString(Map.of("to", to, "model", model));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("failed to serialize notification payload", e);
        }
    }
}
