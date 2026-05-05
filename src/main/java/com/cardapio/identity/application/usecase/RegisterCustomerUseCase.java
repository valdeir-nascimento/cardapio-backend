package com.cardapio.identity.application.usecase;

import com.cardapio.identity.application.command.RegisterCustomerCommand;
import com.cardapio.identity.domain.model.Customer;
import com.cardapio.identity.domain.model.CustomerId;
import com.cardapio.identity.domain.model.HashedPassword;
import com.cardapio.identity.domain.model.RawPassword;
import com.cardapio.identity.domain.port.CustomerRepository;
import com.cardapio.identity.domain.port.PasswordHasher;
import com.cardapio.shared.domain.Email;
import com.cardapio.shared.domain.Notification;
import com.cardapio.shared.domain.PhoneNumber;
import com.cardapio.shared.domain.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterCustomerUseCase {

    private final CustomerRepository repo;
    private final PasswordHasher hasher;

    public RegisterCustomerUseCase(CustomerRepository repo, PasswordHasher hasher) {
        this.repo = repo;
        this.hasher = hasher;
    }

    @Transactional
    public Result<CustomerId> execute(RegisterCustomerCommand cmd) {
        Notification n = Notification.empty();

        Email email = parseEmail(cmd.email(), n);
        PhoneNumber phone = parsePhone(cmd.phoneNumber(), n);
        RawPassword password = parsePassword(cmd.rawPassword(), n);

        if (cmd.name() == null || cmd.name().isBlank()) {
            n.addError("name", "BLANK_NAME", "nome obrigatório");
        }

        if (n.hasErrors()) return Result.failure(n);

        if (repo.existsByEmail(email)) {
            n.addError("email", "EMAIL_ALREADY_REGISTERED", "este e-mail já está cadastrado");
            return Result.failure(n);
        }

        HashedPassword hashed = hasher.hash(password);
        Customer customer = Customer.register(cmd.name(), email, phone, hashed);
        repo.save(customer);
        return Result.success(customer.id());
    }

    private Email parseEmail(String raw, Notification n) {
        try { return Email.of(raw); }
        catch (RuntimeException e) { n.addError("email", "INVALID_EMAIL", "e-mail inválido"); return null; }
    }

    private PhoneNumber parsePhone(String raw, Notification n) {
        try { return PhoneNumber.of(raw); }
        catch (RuntimeException e) { n.addError("phoneNumber", "INVALID_PHONE", "telefone inválido"); return null; }
    }

    private RawPassword parsePassword(String raw, Notification n) {
        try { return RawPassword.of(raw); }
        catch (RuntimeException e) { n.addError("password", "WEAK_PASSWORD", "senha fraca"); return null; }
    }
}
