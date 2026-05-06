package com.cardapio.delivery.domain.model;

import com.cardapio.shared.domain.AggregateRoot;
import com.cardapio.shared.domain.Money;

import java.util.Objects;

public final class Neighborhood extends AggregateRoot<NeighborhoodId> {

    private String name;
    private String city;
    private Money fee;
    private boolean active;

    private Neighborhood(NeighborhoodId id, String name, String city, Money fee, boolean active) {
        super(id);
        setName(name);
        setCity(city);
        setFee(fee);
        this.active = active;
    }

    public static Neighborhood create(String name, String city, Money fee) {
        return new Neighborhood(NeighborhoodId.newId(), name, city, fee, true);
    }

    public static Neighborhood rehydrate(NeighborhoodId id, String name, String city, Money fee, boolean active) {
        return new Neighborhood(id, name, city, fee, active);
    }

    public void rename(String name) { setName(name); }
    public void relocate(String city) { setCity(city); }
    public void changeFee(Money fee) { setFee(fee); }
    public void activate() { this.active = true; }
    public void deactivate() { this.active = false; }

    private void setName(String name) {
        Objects.requireNonNull(name, "name");
        String trimmed = name.trim();
        if (trimmed.isBlank()) throw new IllegalArgumentException("name must not be blank");
        if (trimmed.length() > 120) throw new IllegalArgumentException("name must be at most 120 chars");
        this.name = trimmed;
    }

    private void setCity(String city) {
        Objects.requireNonNull(city, "city");
        String trimmed = city.trim();
        if (trimmed.isBlank()) throw new IllegalArgumentException("city must not be blank");
        if (trimmed.length() > 120) throw new IllegalArgumentException("city must be at most 120 chars");
        this.city = trimmed;
    }

    private void setFee(Money fee) {
        Objects.requireNonNull(fee, "fee");
        this.fee = fee;
    }

    public String name() { return name; }
    public String city() { return city; }
    public Money fee() { return fee; }
    public boolean isActive() { return active; }
}
