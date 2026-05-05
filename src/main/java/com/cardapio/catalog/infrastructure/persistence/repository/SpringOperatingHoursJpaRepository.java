package com.cardapio.catalog.infrastructure.persistence.repository;

import com.cardapio.catalog.infrastructure.persistence.jpa.OperatingHoursJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringOperatingHoursJpaRepository extends JpaRepository<OperatingHoursJpaEntity, UUID> {
    List<OperatingHoursJpaEntity> findAllByOrderByDayOfWeekAscOpenTimeAsc();
    void deleteAllByDayOfWeek(short dayOfWeek);
}
