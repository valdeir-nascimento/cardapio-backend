package com.cardapio.delivery.domain.port;

import com.cardapio.delivery.domain.model.Neighborhood;
import com.cardapio.delivery.domain.model.NeighborhoodId;

import java.util.List;
import java.util.Optional;

public interface NeighborhoodRepository {
    void save(Neighborhood neighborhood);
    Optional<Neighborhood> findById(NeighborhoodId id);
    List<Neighborhood> findAll();
    List<Neighborhood> findAllActive();
    void deleteById(NeighborhoodId id);
    boolean existsById(NeighborhoodId id);
    boolean existsByNameAndCity(String name, String city);
}
