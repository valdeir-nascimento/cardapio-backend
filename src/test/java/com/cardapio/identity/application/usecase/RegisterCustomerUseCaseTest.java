package com.cardapio.identity.application.usecase;

import com.cardapio.identity.application.command.RegisterCustomerCommand;
import com.cardapio.identity.domain.model.Customer;
import com.cardapio.identity.domain.model.CustomerId;
import com.cardapio.identity.domain.model.HashedPassword;
import com.cardapio.identity.domain.model.RawPassword;
import com.cardapio.identity.domain.port.CustomerRepository;
import com.cardapio.identity.domain.port.PasswordHasher;
import com.cardapio.shared.domain.Email;
import com.cardapio.shared.domain.Result;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RegisterCustomerUseCaseTest {

    private final CustomerRepository repo = mock(CustomerRepository.class);
    private final PasswordHasher hasher = mock(PasswordHasher.class);
    private final RegisterCustomerUseCase useCase = new RegisterCustomerUseCase(repo, hasher);

    @Test
    void registersWhenEmailIsNew() {
        when(repo.existsByEmail(any())).thenReturn(false);
        when(hasher.hash(any())).thenReturn(new HashedPassword("$2a$12$x"));

        Result<CustomerId> result = useCase.execute(new RegisterCustomerCommand(
            "Maria", "maria@example.com", "+5511912345678", "S3curePass!"));

        assertThat(result.isSuccess()).isTrue();
        verify(repo).save(any(Customer.class));
    }

    @Test
    void rejectsDuplicateEmail() {
        when(repo.existsByEmail(Email.of("dup@example.com"))).thenReturn(true);

        Result<CustomerId> result = useCase.execute(new RegisterCustomerCommand(
            "X", "dup@example.com", "+5511912345678", "S3curePass!"));

        assertThat(result.isSuccess()).isFalse();
        assertThat(((Result.Failure<CustomerId>) result).notification().errors())
            .extracting("code").contains("EMAIL_ALREADY_REGISTERED");
        verify(repo, never()).save(any());
    }

    @Test
    void rejectsInvalidEmail() {
        Result<CustomerId> result = useCase.execute(new RegisterCustomerCommand(
            "X", "not-an-email", "+5511912345678", "S3curePass!"));

        assertThat(result.isSuccess()).isFalse();
        assertThat(((Result.Failure<CustomerId>) result).notification().errors())
            .extracting("code").contains("INVALID_EMAIL");
    }

    @Test
    void rejectsWeakPassword() {
        Result<CustomerId> result = useCase.execute(new RegisterCustomerCommand(
            "X", "x@y.com", "+5511912345678", "weak"));

        assertThat(result.isSuccess()).isFalse();
        assertThat(((Result.Failure<CustomerId>) result).notification().errors())
            .extracting("code").contains("WEAK_PASSWORD");
    }
}
