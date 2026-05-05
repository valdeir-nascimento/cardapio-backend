package com.cardapio.catalog.domain.port;

import com.cardapio.catalog.domain.model.OperatingHours;

import java.util.Optional;

public interface OperatingHoursRepository {
    void save(OperatingHours hours);
    Optional<OperatingHours> load();
}
