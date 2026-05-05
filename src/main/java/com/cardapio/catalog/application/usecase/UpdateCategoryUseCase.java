package com.cardapio.catalog.application.usecase;

import com.cardapio.catalog.application.command.UpdateCategoryCommand;
import com.cardapio.catalog.domain.model.CategoryId;
import com.cardapio.catalog.domain.port.CategoryRepository;
import com.cardapio.shared.domain.ErrorCode;
import com.cardapio.shared.domain.Notification;
import com.cardapio.shared.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdateCategoryUseCase {

    private final CategoryRepository repo;

    @Transactional
    public Result<CategoryId> execute(UpdateCategoryCommand cmd) {
        return Result.ofOptional(repo.findById(cmd.id()), ErrorCode.CATEGORY_NOT_FOUND)
            .flatMap(c -> {
                Notification n = Notification.empty();
                if (cmd.name() == null || cmd.name().isBlank()) {
                    n.addError("name", ErrorCode.BLANK_NAME);
                }
                if (cmd.displayOrder() < 0) {
                    n.addError("displayOrder", ErrorCode.INVALID_DISPLAY_ORDER);
                }
                if (n.hasErrors()) return Result.failure(n);

                c.rename(cmd.name());
                c.reorder(cmd.displayOrder());
                if (cmd.active()) c.activate(); else c.deactivate();
                repo.save(c);
                return Result.success(c.id());
            });
    }
}
