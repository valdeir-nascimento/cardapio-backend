package com.cardapio.delivery.application.usecase;

import com.cardapio.delivery.application.command.CreateNeighborhoodCommand;
import com.cardapio.delivery.domain.model.Neighborhood;
import com.cardapio.delivery.domain.model.NeighborhoodId;
import com.cardapio.delivery.domain.port.NeighborhoodRepository;
import com.cardapio.shared.domain.ErrorCode;
import com.cardapio.shared.domain.Money;
import com.cardapio.shared.domain.Notification;
import com.cardapio.shared.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateNeighborhoodUseCase {

    private final NeighborhoodRepository repo;

    @Transactional
    public Result<NeighborhoodId> execute(CreateNeighborhoodCommand cmd) {
        Notification n = Notification.empty();
        if (cmd.name() == null || cmd.name().isBlank()) n.addError("name", ErrorCode.BLANK_NAME);
        if (cmd.city() == null || cmd.city().isBlank()) n.addError("city", ErrorCode.BLANK_CITY);
        if (cmd.fee() == null || cmd.fee().signum() < 0) n.addError("fee", ErrorCode.INVALID_FEE);
        if (n.hasErrors()) return Result.failure(n);

        if (repo.existsByNameAndCity(cmd.name().trim(), cmd.city().trim())) {
            return Result.failWith(ErrorCode.NEIGHBORHOOD_ALREADY_EXISTS);
        }

        Neighborhood neighborhood = Neighborhood.create(cmd.name(), cmd.city(), Money.brl(cmd.fee()));
        repo.save(neighborhood);
        return Result.success(neighborhood.id());
    }
}
