package com.cardapio.delivery.application;

import com.cardapio.api.error.NotFoundException;
import com.cardapio.api.error.NotificationException;
import com.cardapio.delivery.application.command.CreateNeighborhoodCommand;
import com.cardapio.delivery.application.command.UpdateNeighborhoodCommand;
import com.cardapio.delivery.application.dto.NeighborhoodView;
import com.cardapio.delivery.application.usecase.CreateNeighborhoodUseCase;
import com.cardapio.delivery.application.usecase.DeleteNeighborhoodUseCase;
import com.cardapio.delivery.application.usecase.GetDeliveryFeeQuery;
import com.cardapio.delivery.application.usecase.ListNeighborhoodsQuery;
import com.cardapio.delivery.application.usecase.UpdateNeighborhoodUseCase;
import com.cardapio.delivery.domain.model.NeighborhoodId;
import com.cardapio.shared.domain.Money;
import com.cardapio.shared.domain.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DeliveryFacadeImpl implements DeliveryFacade {

    private final CreateNeighborhoodUseCase createUseCase;
    private final UpdateNeighborhoodUseCase updateUseCase;
    private final DeleteNeighborhoodUseCase deleteUseCase;
    private final ListNeighborhoodsQuery listQuery;
    private final GetDeliveryFeeQuery feeQuery;

    @Override
    public NeighborhoodId createNeighborhood(CreateNeighborhoodCommand cmd) {
        return createUseCase.execute(cmd).orElseThrow(DeliveryFacadeImpl::toException);
    }

    @Override
    public NeighborhoodId updateNeighborhood(UpdateNeighborhoodCommand cmd) {
        return updateUseCase.execute(cmd).orElseThrow(DeliveryFacadeImpl::toException);
    }

    @Override
    public void deleteNeighborhood(NeighborhoodId id) {
        deleteUseCase.execute(id).orElseThrow(DeliveryFacadeImpl::toException);
    }

    @Override
    public List<NeighborhoodView> listAll() {
        return listQuery.all();
    }

    @Override
    public List<NeighborhoodView> listActive() {
        return listQuery.active();
    }

    @Override
    public Optional<NeighborhoodView> getById(NeighborhoodId id) {
        return feeQuery.getById(id);
    }

    @Override
    public Optional<Money> getActiveFee(NeighborhoodId id) {
        return feeQuery.getActiveFee(id);
    }

    private static RuntimeException toException(Notification n) {
        boolean notFound = n.errors().stream()
            .anyMatch(e -> e.code() != null && e.code().endsWith("_NOT_FOUND"));
        return notFound ? new NotFoundException(n) : new NotificationException(n);
    }
}
