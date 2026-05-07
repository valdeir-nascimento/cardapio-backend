package com.cardapio.identity.application.usecase;

import com.cardapio.identity.application.command.DeleteMyAccountCommand;
import com.cardapio.identity.domain.model.Customer;
import com.cardapio.identity.domain.model.CustomerId;
import com.cardapio.identity.domain.model.HashedPassword;
import com.cardapio.identity.domain.port.CustomerRepository;
import com.cardapio.identity.domain.port.RefreshTokenRepository;
import com.cardapio.shared.domain.Email;
import com.cardapio.shared.domain.PhoneNumber;
import com.cardapio.shared.domain.Result;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeleteMyAccountUseCaseTest {

    private final CustomerRepository customers = mock(CustomerRepository.class);
    private final RefreshTokenRepository refreshTokens = mock(RefreshTokenRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-05-06T12:00:00Z"), ZoneOffset.UTC);
    private final DeleteMyAccountUseCase useCase = new DeleteMyAccountUseCase(customers, refreshTokens, clock);

    @Test
    void anonymizesAndRevokesRefreshTokens() {
        Customer customer = Customer.register("Maria",
            Email.of("maria@example.com"),
            PhoneNumber.of("+5511912345678"),
            HashedPassword.of("$2a$12$hash"));
        when(customers.findById(customer.id())).thenReturn(Optional.of(customer));

        Result<Void> result = useCase.execute(new DeleteMyAccountCommand(customer.id()));

        assertThat(result.isSuccess()).isTrue();
        assertThat(customer.isDeleted()).isTrue();
        verify(customers).save(customer);
        verify(refreshTokens).revokeAllForSubject(customer.id().value());
    }

    @Test
    void rejectsUnknownCustomer() {
        CustomerId id = CustomerId.newId();
        when(customers.findById(id)).thenReturn(Optional.empty());

        Result<Void> result = useCase.execute(new DeleteMyAccountCommand(id));

        assertThat(result.isSuccess()).isFalse();
        assertThat(((Result.Failure<Void>) result).notification().errors())
            .extracting("code").contains("CUSTOMER_NOT_FOUND");
        verify(refreshTokens, never()).revokeAllForSubject(id.value());
    }

    @Test
    void rejectsAlreadyDeleted() {
        Customer customer = Customer.register("X",
            Email.of("x@y.com"),
            PhoneNumber.of("+5511912345678"),
            HashedPassword.of("$2a$12$hash"));
        customer.anonymize(clock);
        when(customers.findById(customer.id())).thenReturn(Optional.of(customer));

        Result<Void> result = useCase.execute(new DeleteMyAccountCommand(customer.id()));

        assertThat(result.isSuccess()).isFalse();
        assertThat(((Result.Failure<Void>) result).notification().errors())
            .extracting("code").contains("ACCOUNT_ALREADY_DELETED");
        verify(customers, never()).save(customer);
        verify(refreshTokens, times(0)).revokeAllForSubject(customer.id().value());
    }
}
