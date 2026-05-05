package com.cardapio.catalog.infrastructure.persistence.adapter;

import com.cardapio.catalog.domain.model.OperatingHours;
import com.cardapio.catalog.domain.port.OperatingHoursRepository;
import com.cardapio.catalog.infrastructure.persistence.mapper.OperatingHoursMapper;
import com.cardapio.catalog.infrastructure.persistence.repository.SpringOperatingHoursJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OperatingHoursRepositoryAdapter implements OperatingHoursRepository {

    private final SpringOperatingHoursJpaRepository jpa;

    @Override
    @Transactional
    public void save(OperatingHours hours) {
        // simple: drop all rows and rewrite
        jpa.deleteAllInBatch();
        jpa.saveAll(OperatingHoursMapper.toJpaRows(hours));
    }

    @Override
    public Optional<OperatingHours> load() {
        var rows = jpa.findAllByOrderByDayOfWeekAscOpenTimeAsc();
        return rows.isEmpty() ? Optional.empty() : Optional.of(OperatingHoursMapper.toDomain(rows));
    }
}
