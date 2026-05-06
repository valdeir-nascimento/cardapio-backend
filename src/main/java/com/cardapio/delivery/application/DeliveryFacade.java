package com.cardapio.delivery.application;

import com.cardapio.api.error.NotificationException;
import com.cardapio.delivery.application.command.CreateNeighborhoodCommand;
import com.cardapio.delivery.application.command.UpdateNeighborhoodCommand;
import com.cardapio.delivery.application.dto.NeighborhoodView;
import com.cardapio.delivery.domain.model.NeighborhoodId;
import com.cardapio.shared.domain.Money;
import org.springframework.modulith.NamedInterface;

import java.util.List;
import java.util.Optional;

@NamedInterface("DeliveryFacade")
public interface DeliveryFacade {

    NeighborhoodId createNeighborhood(CreateNeighborhoodCommand cmd) throws NotificationException;

    NeighborhoodId updateNeighborhood(UpdateNeighborhoodCommand cmd) throws NotificationException;

    void deleteNeighborhood(NeighborhoodId id) throws NotificationException;

    List<NeighborhoodView> listAll();

    List<NeighborhoodView> listActive();

    Optional<NeighborhoodView> getById(NeighborhoodId id);

    Optional<Money> getActiveFee(NeighborhoodId id);
}
