package com.cardapio.catalog.api.rest;

import com.cardapio.catalog.api.dto.OperatingHoursResponse;
import com.cardapio.catalog.application.CatalogFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/operating-hours")
@RequiredArgsConstructor
public class PublicOperatingHoursController implements PublicOperatingHoursApi {

    private final CatalogFacade catalog;

    @Override
    @GetMapping
    public OperatingHoursResponse get() {
        return OperatingHoursResponse.from(catalog.getOperatingHours());
    }
}
