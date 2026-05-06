package com.cardapio.payment.application.usecase;

import com.cardapio.payment.application.dto.PaymentView;
import com.cardapio.payment.domain.model.PaymentTransactionId;
import com.cardapio.payment.domain.port.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GetPaymentQuery {

    private final PaymentTransactionRepository repo;

    @Transactional(readOnly = true)
    public Optional<PaymentView> getForCustomer(PaymentTransactionId id, UUID customerId) {
        return repo.findById(id)
            .filter(tx -> tx.customerId().equals(customerId))
            .map(PaymentView::from);
    }

    @Transactional(readOnly = true)
    public Optional<PaymentView> getAdmin(PaymentTransactionId id) {
        return repo.findById(id).map(PaymentView::from);
    }
}
