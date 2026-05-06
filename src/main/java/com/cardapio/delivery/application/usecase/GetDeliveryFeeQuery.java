package com.cardapio.delivery.application.usecase;

import com.cardapio.delivery.application.dto.NeighborhoodView;
import com.cardapio.delivery.domain.model.NeighborhoodId;
import com.cardapio.delivery.domain.port.NeighborhoodRepository;
import com.cardapio.shared.domain.Money;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GetDeliveryFeeQuery {

    private final NeighborhoodRepository repo;

    @Transactional(readOnly = true)
    public Optional<NeighborhoodView> getById(NeighborhoodId id) {
        return repo.findById(id).map(NeighborhoodView::from);
    }

    @Transactional(readOnly = true)
    public Optional<Money> getActiveFee(NeighborhoodId id) {
        return repo.findById(id).filter(n -> n.isActive()).map(n -> n.fee());
    }
}
