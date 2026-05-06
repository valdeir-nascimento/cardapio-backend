package com.cardapio.notification.application.scheduler;

import com.cardapio.notification.application.usecase.DispatchOutboxUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "notification.dispatch.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
class OutboxDispatchScheduler {

    private final DispatchOutboxUseCase dispatcher;

    @Scheduled(
        fixedDelayString = "${notification.dispatch.fixed-delay-ms:30000}",
        initialDelayString = "${notification.dispatch.initial-delay-ms:10000}"
    )
    void tick() {
        try {
            int processed = dispatcher.run();
            if (processed > 0) {
                log.debug("notification dispatcher processed {} rows", processed);
            }
        } catch (Exception e) {
            log.error("notification dispatcher tick failed", e);
        }
    }
}
