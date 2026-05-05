package com.cardapio.catalog.application.usecase;

import com.cardapio.catalog.application.dto.CategorySummaryView;
import com.cardapio.catalog.domain.port.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListCategoriesQuery {

    private final CategoryRepository repo;

    @Transactional(readOnly = true)
    public List<CategorySummaryView> execute() {
        return repo.findAll().stream()
            .map(c -> new CategorySummaryView(c.id(), c.name(), c.displayOrder(), c.isActive()))
            .toList();
    }
}
