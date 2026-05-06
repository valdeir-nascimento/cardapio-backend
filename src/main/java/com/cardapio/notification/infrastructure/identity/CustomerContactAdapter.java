package com.cardapio.notification.infrastructure.identity;

import com.cardapio.api.error.NotFoundException;
import com.cardapio.identity.application.IdentityFacade;
import com.cardapio.identity.application.dto.CustomerProfile;
import com.cardapio.identity.domain.model.CustomerId;
import com.cardapio.notification.domain.model.CustomerContact;
import com.cardapio.notification.domain.port.CustomerContactPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class CustomerContactAdapter implements CustomerContactPort {

    private final IdentityFacade identityFacade;

    @Override
    public Optional<CustomerContact> find(UUID customerId) {
        try {
            CustomerProfile p = identityFacade.getMyProfile(CustomerId.of(customerId));
            return Optional.of(new CustomerContact(p.id().value(), p.name(), p.email(), p.phoneNumber()));
        } catch (NotFoundException e) {
            return Optional.empty();
        }
    }
}
