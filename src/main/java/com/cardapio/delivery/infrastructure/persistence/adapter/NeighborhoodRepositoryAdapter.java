package com.cardapio.delivery.infrastructure.persistence.adapter;

import com.cardapio.delivery.domain.model.Neighborhood;
import com.cardapio.delivery.domain.model.NeighborhoodId;
import com.cardapio.delivery.domain.port.NeighborhoodRepository;
import com.cardapio.delivery.infrastructure.persistence.mapper.NeighborhoodMapper;
import com.cardapio.delivery.infrastructure.persistence.repository.SpringNeighborhoodJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class NeighborhoodRepositoryAdapter implements NeighborhoodRepository {

    private final SpringNeighborhoodJpaRepository jpa;
    private final Clock clock;

    @Override
    public void save(Neighborhood neighborhood) {
        var existing = jpa.findById(neighborhood.id().value());
        if (existing.isPresent()) {
            NeighborhoodMapper.updateJpa(existing.get(), neighborhood, clock.instant());
            jpa.save(existing.get());
        } else {
            jpa.save(NeighborhoodMapper.toJpa(neighborhood, clock.instant()));
        }
    }

    @Override
    public Optional<Neighborhood> findById(NeighborhoodId id) {
        return jpa.findById(id.value()).map(NeighborhoodMapper::toDomain);
    }

    @Override
    public List<Neighborhood> findAll() {
        return jpa.findAllByOrderByCityAscNameAsc().stream().map(NeighborhoodMapper::toDomain).toList();
    }

    @Override
    public List<Neighborhood> findAllActive() {
        return jpa.findAllByActiveTrueOrderByCityAscNameAsc().stream().map(NeighborhoodMapper::toDomain).toList();
    }

    @Override
    public void deleteById(NeighborhoodId id) {
        jpa.deleteById(id.value());
    }

    @Override
    public boolean existsById(NeighborhoodId id) {
        return jpa.existsById(id.value());
    }

    @Override
    public boolean existsByNameAndCity(String name, String city) {
        return jpa.existsByNameAndCity(name, city);
    }
}
