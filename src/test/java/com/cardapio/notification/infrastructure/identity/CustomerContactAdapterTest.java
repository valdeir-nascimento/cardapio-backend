package com.cardapio.notification.infrastructure.identity;

import com.cardapio.api.error.NotFoundException;
import com.cardapio.identity.application.IdentityFacade;
import com.cardapio.identity.application.dto.CustomerProfile;
import com.cardapio.identity.domain.model.CustomerId;
import com.cardapio.notification.domain.model.CustomerContact;
import com.cardapio.shared.domain.Notification;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomerContactAdapterTest {

    @Test
    void mapsProfileToContact() {
        IdentityFacade facade = mock(IdentityFacade.class);
        UUID customerId = UUID.randomUUID();
        when(facade.getMyProfile(any(CustomerId.class)))
            .thenReturn(new CustomerProfile(CustomerId.of(customerId), "Maria", "maria@x.com", "+5511999998888"));

        CustomerContactAdapter adapter = new CustomerContactAdapter(facade);
        Optional<CustomerContact> result = adapter.find(customerId);

        assertThat(result).isPresent();
        assertThat(result.get().email()).isEqualTo("maria@x.com");
        assertThat(result.get().phoneNumber()).isEqualTo("+5511999998888");
    }

    @Test
    void returnsEmptyOnNotFound() {
        IdentityFacade facade = mock(IdentityFacade.class);
        when(facade.getMyProfile(any(CustomerId.class)))
            .thenThrow(new NotFoundException(Notification.empty()));

        CustomerContactAdapter adapter = new CustomerContactAdapter(facade);
        assertThat(adapter.find(UUID.randomUUID())).isEmpty();
    }
}
