package com.cardapio.catalog.application.usecase;

import com.cardapio.catalog.application.command.CreateCategoryCommand;
import com.cardapio.catalog.domain.model.Category;
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
public class CreateCategoryUseCase {

    private final CategoryRepository repo;

    @Transactional
    public Result<CategoryId> execute(CreateCategoryCommand cmd) {
        Notification n = Notification.empty();
        if (cmd.name() == null || cmd.name().isBlank()) {
            n.addError("name", ErrorCode.BLANK_NAME);
        }
        if (cmd.displayOrder() < 0) {
            n.addError("displayOrder", ErrorCode.INVALID_DISPLAY_ORDER);
        }
        if (n.hasErrors()) return Result.failure(n);

        Category category = Category.create(cmd.name(), cmd.displayOrder());
        repo.save(category);
        return Result.success(category.id());
    }
}
