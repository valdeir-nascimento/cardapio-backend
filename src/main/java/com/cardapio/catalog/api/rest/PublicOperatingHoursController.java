package com.cardapio.catalog.api.rest;

import com.cardapio.catalog.api.dto.OperatingHoursResponse;
import com.cardapio.catalog.application.usecase.GetOperatingHoursQuery;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/operating-hours")
public class PublicOperatingHoursController {

    private final GetOperatingHoursQuery query;
    public PublicOperatingHoursController(GetOperatingHoursQuery query) { this.query = query; }

    @GetMapping
    public OperatingHoursResponse get() {
        return OperatingHoursResponse.from(query.execute());
    }
}
