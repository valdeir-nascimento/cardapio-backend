package com.cardapio.delivery.application.usecase;

import com.cardapio.delivery.domain.model.NeighborhoodId;
import com.cardapio.delivery.domain.port.NeighborhoodRepository;
import com.cardapio.shared.domain.ErrorCode;
import com.cardapio.shared.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeleteNeighborhoodUseCase {

    private final NeighborhoodRepository repo;

    @Transactional
    public Result<Void> execute(NeighborhoodId id) {
        if (!repo.existsById(id)) return Result.failWith(ErrorCode.NEIGHBORHOOD_NOT_FOUND);
        repo.deleteById(id);
        return Result.ok();
    }
}
