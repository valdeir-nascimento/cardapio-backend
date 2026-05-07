package com.cardapio.identity.application.usecase;

import com.cardapio.identity.application.dto.CustomerProfile;
import com.cardapio.identity.domain.model.CustomerId;
import com.cardapio.identity.domain.port.CustomerRepository;
import com.cardapio.shared.domain.ErrorCode;
import com.cardapio.shared.domain.PhoneNumber;
import com.cardapio.shared.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetMyProfileUseCase {

    private final CustomerRepository customers;

    @Transactional(readOnly = true)
    public Result<CustomerProfile> execute(CustomerId id) {
        return Result.ofOptional(customers.findById(id), ErrorCode.CUSTOMER_NOT_FOUND)
            .map(c -> new CustomerProfile(c.id(), c.name(), c.email().value(),
                c.phoneNumber().map(PhoneNumber::value).orElse(null)));
    }
}
