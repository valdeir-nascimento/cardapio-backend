package com.cardapio.identity.application.usecase;

import com.cardapio.identity.application.dto.CustomerProfile;
import com.cardapio.identity.domain.model.Customer;
import com.cardapio.identity.domain.model.CustomerId;
import com.cardapio.identity.domain.port.CustomerRepository;
import com.cardapio.shared.domain.Notification;
import com.cardapio.shared.domain.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetMyProfileUseCase {
    private final CustomerRepository customers;
    public GetMyProfileUseCase(CustomerRepository customers) { this.customers = customers; }

    @Transactional(readOnly = true)
    public Result<CustomerProfile> execute(CustomerId id) {
        return customers.findById(id)
            .map(c -> Result.success(new CustomerProfile(c.id(), c.name(), c.email().value(), c.phoneNumber().value())))
            .orElseGet(() -> {
                Notification n = Notification.empty();
                n.addError("CUSTOMER_NOT_FOUND", "cliente não encontrado");
                return Result.failure(n);
            });
    }
}
