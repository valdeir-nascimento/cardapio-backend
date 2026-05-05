package com.cardapio.catalog.application.usecase;

import com.cardapio.catalog.domain.model.CategoryId;
import com.cardapio.catalog.domain.port.CategoryRepository;
import com.cardapio.shared.domain.ErrorCode;
import com.cardapio.shared.domain.Money;
import com.cardapio.shared.domain.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductCommandValidator {

    private final CategoryRepository categories;

    public void validate(String name, Money basePrice, CategoryId categoryId, Notification n) {
        if (name == null || name.isBlank()) {
            n.addError("name", ErrorCode.BLANK_NAME);
        }
        if (basePrice == null) {
            n.addError("basePrice", ErrorCode.INVALID_PRICE);
        }
        if (categoryId == null || !categories.existsById(categoryId)) {
            n.addError("categoryId", ErrorCode.CATEGORY_NOT_FOUND);
        }
    }
}
