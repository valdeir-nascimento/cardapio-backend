package com.cardapio.delivery.infrastructure.persistence.repository;

import com.cardapio.delivery.infrastructure.persistence.jpa.NeighborhoodJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringNeighborhoodJpaRepository extends JpaRepository<NeighborhoodJpaEntity, UUID> {
    List<NeighborhoodJpaEntity> findAllByOrderByCityAscNameAsc();
    List<NeighborhoodJpaEntity> findAllByActiveTrueOrderByCityAscNameAsc();
    boolean existsByNameAndCity(String name, String city);
}
