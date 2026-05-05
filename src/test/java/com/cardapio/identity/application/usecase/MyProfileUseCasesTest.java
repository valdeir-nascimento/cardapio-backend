package com.cardapio.identity.application.usecase;

import com.cardapio.identity.application.command.UpdateProfileCommand;
import com.cardapio.identity.application.dto.CustomerProfile;
import com.cardapio.identity.domain.model.Customer;
import com.cardapio.identity.domain.model.CustomerId;
import com.cardapio.identity.domain.model.HashedPassword;
import com.cardapio.identity.domain.port.CustomerRepository;
import com.cardapio.shared.domain.Email;
import com.cardapio.shared.domain.PhoneNumber;
import com.cardapio.shared.domain.Result;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class MyProfileUseCasesTest {

    private final CustomerRepository repo = mock(CustomerRepository.class);

    @Test
    void getProfileReturnsDto() {
        CustomerId id = CustomerId.newId();
        Customer c = Customer.rehydrate(id, "Maria", Email.of("m@x.com"),
            PhoneNumber.of("+5511912345678"), new HashedPassword("$2a$12$x"));
        when(repo.findById(id)).thenReturn(Optional.of(c));

        Result<CustomerProfile> r = new GetMyProfileUseCase(repo).execute(id);
        assertThat(r.isSuccess()).isTrue();
        assertThat(r.getOrThrow().email()).isEqualTo("m@x.com");
    }

    @Test
    void updateProfileSavesChanges() {
        CustomerId id = CustomerId.newId();
        Customer c = Customer.rehydrate(id, "Maria", Email.of("m@x.com"),
            PhoneNumber.of("+5511912345678"), new HashedPassword("$2a$12$x"));
        when(repo.findById(id)).thenReturn(Optional.of(c));

        var useCase = new UpdateMyProfileUseCase(repo);
        Result<CustomerProfile> r = useCase.execute(new UpdateProfileCommand(id, "Maria Nova", "+5511987654321"));

        assertThat(r.isSuccess()).isTrue();
        assertThat(r.getOrThrow().name()).isEqualTo("Maria Nova");
        verify(repo).save(any(Customer.class));
    }

    @Test
    void updateRejectsInvalidPhone() {
        when(repo.findById(any())).thenReturn(Optional.of(Customer.rehydrate(
            CustomerId.newId(), "X", Email.of("x@y.com"),
            PhoneNumber.of("+5511912345678"), new HashedPassword("$2a$12$x"))));

        Result<CustomerProfile> r = new UpdateMyProfileUseCase(repo)
            .execute(new UpdateProfileCommand(CustomerId.newId(), "X", "abc"));
        assertThat(r.isSuccess()).isFalse();
    }
}
