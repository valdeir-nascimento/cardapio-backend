package com.cardapio.identity.application.event;

import com.cardapio.identity.domain.event.CustomerRegistered;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CustomerRegisteredHandler {

    @EventListener
    public void onCustomerRegistered(CustomerRegistered event) {
        log.info("Customer registered: id={} email={}", event.customerId(), event.email().value());
    }
}
