package com.cardapio.catalog.infrastructure.persistence.jpa;

import jakarta.persistence.*;

import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "operating_hours")
public class OperatingHoursJpaEntity {

    @Id @Column(nullable = false) private UUID id;
    @Column(name = "day_of_week", nullable = false) private short dayOfWeek;  // 1..7
    @Column(name = "open_time", nullable = false) private LocalTime openTime;
    @Column(name = "close_time", nullable = false) private LocalTime closeTime;

    protected OperatingHoursJpaEntity() {}

    public OperatingHoursJpaEntity(UUID id, short dayOfWeek, LocalTime openTime, LocalTime closeTime) {
        this.id = id; this.dayOfWeek = dayOfWeek; this.openTime = openTime; this.closeTime = closeTime;
    }

    public UUID getId() { return id; }
    public short getDayOfWeek() { return dayOfWeek; }
    public LocalTime getOpenTime() { return openTime; }
    public LocalTime getCloseTime() { return closeTime; }
}
