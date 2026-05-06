package com.cardapio.notification.domain.port;

import com.cardapio.notification.domain.model.CustomerContact;

import java.util.Optional;
import java.util.UUID;

public interface CustomerContactPort {

    Optional<CustomerContact> find(UUID customerId);
}
