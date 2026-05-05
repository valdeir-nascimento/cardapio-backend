// UpdateCategoryUseCase.java
package com.cardapio.catalog.application.usecase;

import com.cardapio.catalog.application.command.UpdateCategoryCommand;
import com.cardapio.catalog.domain.model.Category;
import com.cardapio.catalog.domain.model.CategoryId;
import com.cardapio.catalog.domain.port.CategoryRepository;
import com.cardapio.shared.domain.Notification;
import com.cardapio.shared.domain.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UpdateCategoryUseCase {
    private final CategoryRepository repo;
    public UpdateCategoryUseCase(CategoryRepository repo) { this.repo = repo; }

    @Transactional
    public Result<CategoryId> execute(UpdateCategoryCommand cmd) {
        Notification n = Notification.empty();
        Optional<Category> maybe = repo.findById(cmd.id());
        if (maybe.isEmpty()) {
            n.addError("CATEGORY_NOT_FOUND", "categoria não encontrada");
            return Result.failure(n);
        }
        if (cmd.name() == null || cmd.name().isBlank()) n.addError("name", "BLANK_NAME", "nome obrigatório");
        if (cmd.displayOrder() < 0) n.addError("displayOrder", "INVALID_DISPLAY_ORDER", "ordem inválida");
        if (n.hasErrors()) return Result.failure(n);

        Category c = maybe.get();
        c.rename(cmd.name());
        c.reorder(cmd.displayOrder());
        if (cmd.active()) c.activate(); else c.deactivate();
        repo.save(c);
        return Result.success(c.id());
    }
}
