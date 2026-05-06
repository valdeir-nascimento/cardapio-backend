package com.cardapio.ordering.infrastructure.persistence.adapter;

import com.cardapio.ordering.domain.model.Order;
import com.cardapio.ordering.domain.model.OrderId;
import com.cardapio.ordering.domain.model.OrderStatus;
import com.cardapio.ordering.domain.port.OrderRepository;
import com.cardapio.ordering.infrastructure.persistence.jpa.OrderJpaEntity;
import com.cardapio.ordering.infrastructure.persistence.mapper.OrderMapper;
import com.cardapio.ordering.infrastructure.persistence.repository.SpringOrderJpaRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OrderRepositoryAdapter implements OrderRepository {

    private final SpringOrderJpaRepository jpa;

    @Override
    public void save(Order order) {
        var existing = jpa.findById(order.id().value());
        if (existing.isPresent()) {
            OrderMapper.updateStatus(existing.get(), order);
            jpa.save(existing.get());
        } else {
            jpa.save(OrderMapper.toJpa(order));
        }
    }

    @Override
    public Optional<Order> findById(OrderId id) {
        return jpa.findById(id.value()).map(OrderMapper::toDomain);
    }

    @Override
    public List<Order> findByCustomerId(UUID customerId, int limit, int offset) {
        int page = offset / Math.max(1, limit);
        return jpa.findAllByCustomerIdOrderByPlacedAtDesc(customerId, PageRequest.of(page, limit))
            .stream().map(OrderMapper::toDomain).toList();
    }

    @Override
    public List<Order> findAdmin(OrderStatus status, Instant from, Instant to, int limit, int offset) {
        int page = offset / Math.max(1, limit);
        var pageable = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "placedAt"));
        return jpa.findAll(adminSpec(status, from, to), pageable)
            .map(OrderMapper::toDomain)
            .toList();
    }

    @Override
    public long countAdmin(OrderStatus status, Instant from, Instant to) {
        return jpa.count(adminSpec(status, from, to));
    }

    private Specification<OrderJpaEntity> adminSpec(OrderStatus status, Instant from, Instant to) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) predicates.add(cb.equal(root.get("status"), status.name()));
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("placedAt"), from));
            if (to != null) predicates.add(cb.lessThanOrEqualTo(root.get("placedAt"), to));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
