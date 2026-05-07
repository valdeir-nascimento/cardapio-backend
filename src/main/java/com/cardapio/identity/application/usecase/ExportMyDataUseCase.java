package com.cardapio.identity.application.usecase;

import com.cardapio.identity.application.dto.CustomerDataExport;
import com.cardapio.identity.domain.model.Customer;
import com.cardapio.identity.domain.model.CustomerId;
import com.cardapio.identity.domain.port.CustomerRepository;
import com.cardapio.shared.domain.ErrorCode;
import com.cardapio.shared.domain.PhoneNumber;
import com.cardapio.shared.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExportMyDataUseCase {

    private final CustomerRepository customers;
    private final Clock clock;

    @Transactional(readOnly = true)
    public Result<CustomerDataExport> execute(CustomerId customerId) {
        Customer customer = customers.findById(customerId).orElse(null);
        if (customer == null) return Result.failWith(ErrorCode.CUSTOMER_NOT_FOUND);

        List<CustomerDataExport.SocialIdentitySummary> socials = customer.socialIdentities().stream()
            .map(s -> new CustomerDataExport.SocialIdentitySummary(s.provider(), s.emailAtLink(), s.linkedAt()))
            .toList();

        return Result.success(new CustomerDataExport(
            customer.id().value(),
            customer.name(),
            customer.email().value(),
            customer.phoneNumber().map(PhoneNumber::value).orElse(null),
            socials,
            customer.deletedAt().orElse(null),
            clock.instant()
        ));
    }
}
