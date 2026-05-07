package com.cardapio.identity.application.usecase;

import com.cardapio.identity.application.command.DeleteMyAccountCommand;
import com.cardapio.identity.domain.model.Customer;
import com.cardapio.identity.domain.port.CustomerRepository;
import com.cardapio.identity.domain.port.RefreshTokenRepository;
import com.cardapio.shared.domain.ErrorCode;
import com.cardapio.shared.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@RequiredArgsConstructor
public class DeleteMyAccountUseCase {

    private final CustomerRepository customers;
    private final RefreshTokenRepository refreshTokens;
    private final Clock clock;

    @Transactional
    public Result<Void> execute(DeleteMyAccountCommand cmd) {
        Customer customer = customers.findById(cmd.customerId()).orElse(null);
        if (customer == null) return Result.failWith(ErrorCode.CUSTOMER_NOT_FOUND);
        if (customer.isDeleted()) return Result.failWith(ErrorCode.ACCOUNT_ALREADY_DELETED);

        customer.anonymize(clock);
        customers.save(customer);
        refreshTokens.revokeAllForSubject(cmd.customerId().value());
        return Result.ok();
    }
}
