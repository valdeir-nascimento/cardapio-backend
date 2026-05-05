package com.cardapio.identity.application.usecase;

import com.cardapio.identity.application.command.UpdateProfileCommand;
import com.cardapio.identity.application.dto.CustomerProfile;
import com.cardapio.identity.domain.model.Customer;
import com.cardapio.identity.domain.port.CustomerRepository;
import com.cardapio.shared.domain.ErrorCode;
import com.cardapio.shared.domain.Notification;
import com.cardapio.shared.domain.PhoneNumber;
import com.cardapio.shared.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UpdateMyProfileUseCase {

    private final CustomerRepository customers;

    @Transactional
    public Result<CustomerProfile> execute(UpdateProfileCommand cmd) {
        Notification n = Notification.empty();

        if (cmd.name() == null || cmd.name().isBlank()) {
            n.addError("name", ErrorCode.BLANK_NAME);
        }

        PhoneNumber phone = null;
        try {
            phone = PhoneNumber.of(cmd.phoneNumber());
        } catch (RuntimeException e) {
            n.addError("phoneNumber", ErrorCode.INVALID_PHONE);
        }

        Optional<Customer> maybe = customers.findById(cmd.customerId());
        if (maybe.isEmpty()) {
            n.addError(ErrorCode.CUSTOMER_NOT_FOUND);
        }

        if (n.hasErrors()) return Result.failure(n);

        Customer customer = maybe.get();
        customer.updateProfile(cmd.name(), phone);
        customers.save(customer);

        return Result.success(new CustomerProfile(customer.id(), customer.name(), customer.email().value(), customer.phoneNumber().value()));
    }
}
