package com.cardapio.delivery.application.usecase;

import com.cardapio.delivery.application.command.UpdateNeighborhoodCommand;
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
public class UpdateNeighborhoodUseCase {

    private final NeighborhoodRepository repo;

    @Transactional
    public Result<NeighborhoodId> execute(UpdateNeighborhoodCommand cmd) {
        return Result.ofOptional(repo.findById(cmd.id()), ErrorCode.NEIGHBORHOOD_NOT_FOUND)
            .flatMap(n -> {
                Notification notif = Notification.empty();
                if (cmd.name() == null || cmd.name().isBlank()) notif.addError("name", ErrorCode.BLANK_NAME);
                if (cmd.city() == null || cmd.city().isBlank()) notif.addError("city", ErrorCode.BLANK_CITY);
                if (cmd.fee() == null || cmd.fee().signum() < 0) notif.addError("fee", ErrorCode.INVALID_FEE);
                if (notif.hasErrors()) return Result.failure(notif);

                n.rename(cmd.name());
                n.relocate(cmd.city());
                n.changeFee(Money.brl(cmd.fee()));
                if (cmd.active()) n.activate(); else n.deactivate();
                repo.save(n);
                return Result.success(n.id());
            });
    }
}
