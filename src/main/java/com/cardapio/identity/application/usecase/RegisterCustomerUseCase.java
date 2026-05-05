package com.cardapio.identity.application.usecase;

import com.cardapio.identity.application.command.RegisterCustomerCommand;
import com.cardapio.identity.domain.model.Customer;
import com.cardapio.identity.domain.model.CustomerId;
import com.cardapio.identity.domain.model.HashedPassword;
import com.cardapio.identity.domain.model.RawPassword;
import com.cardapio.identity.domain.port.CustomerRepository;
import com.cardapio.identity.domain.port.PasswordHasher;
import com.cardapio.shared.domain.Email;
import com.cardapio.shared.domain.ErrorCode;
import com.cardapio.shared.domain.Notification;
import com.cardapio.shared.domain.PhoneNumber;
import com.cardapio.shared.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RegisterCustomerUseCase {

    private final CustomerRepository repo;
    private final PasswordHasher hasher;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Result<CustomerId> execute(RegisterCustomerCommand cmd) {
        Notification n = Notification.empty();

        Email email = parseEmail(cmd.email(), n);
        PhoneNumber phone = parsePhone(cmd.phoneNumber(), n);
        RawPassword password = parsePassword(cmd.rawPassword(), n);

        if (cmd.name() == null || cmd.name().isBlank()) {
            n.addError("name", ErrorCode.BLANK_NAME);
        }

        if (n.hasErrors()) return Result.failure(n);

        if (repo.existsByEmail(email)) {
            Notification dup = Notification.empty();
            dup.addError("email", ErrorCode.EMAIL_ALREADY_REGISTERED);
            return Result.failure(dup);
        }

        HashedPassword hashed = hasher.hash(password);
        Customer customer = Customer.register(cmd.name(), email, phone, hashed);
        repo.save(customer);
        customer.pullDomainEvents().forEach(eventPublisher::publishEvent);

        return Result.success(customer.id());
    }

    private Email parseEmail(String raw, Notification n) {
        try {
            return Email.of(raw);
        } catch (RuntimeException e) {
            n.addError("email", ErrorCode.INVALID_EMAIL);
            return null;
        }
    }

    private PhoneNumber parsePhone(String raw, Notification n) {
        try {
            return PhoneNumber.of(raw);
        } catch (RuntimeException e) {
            n.addError("phoneNumber", ErrorCode.INVALID_PHONE);
            return null;
        }
    }

    private RawPassword parsePassword(String raw, Notification n) {
        try {
            return RawPassword.of(raw);
        } catch (RuntimeException e) {
            n.addError("password", ErrorCode.WEAK_PASSWORD);
            return null;
        }
    }
}
