package com.cardapio.identity.application.usecase;

import com.cardapio.identity.application.command.UpdateProfileCommand;
import com.cardapio.identity.application.dto.CustomerProfile;
import com.cardapio.identity.domain.model.Customer;
import com.cardapio.identity.domain.port.CustomerRepository;
import com.cardapio.shared.domain.Notification;
import com.cardapio.shared.domain.PhoneNumber;
import com.cardapio.shared.domain.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UpdateMyProfileUseCase {
    private final CustomerRepository customers;
    public UpdateMyProfileUseCase(CustomerRepository customers) { this.customers = customers; }

    @Transactional
    public Result<CustomerProfile> execute(UpdateProfileCommand cmd) {
        Notification n = Notification.empty();

        if (cmd.name() == null || cmd.name().isBlank()) n.addError("name", "BLANK_NAME", "nome obrigatório");

        PhoneNumber phone = null;
        try { phone = PhoneNumber.of(cmd.phoneNumber()); }
        catch (RuntimeException e) { n.addError("phoneNumber", "INVALID_PHONE", "telefone inválido"); }

        Optional<Customer> maybe = customers.findById(cmd.customerId());
        if (maybe.isEmpty()) n.addError("CUSTOMER_NOT_FOUND", "cliente não encontrado");

        if (n.hasErrors()) return Result.failure(n);

        Customer c = maybe.get();
        c.updateProfile(cmd.name(), phone);
        customers.save(c);
        return Result.success(new CustomerProfile(c.id(), c.name(), c.email().value(), c.phoneNumber().value()));
    }
}
