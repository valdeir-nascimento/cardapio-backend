package com.cardapio.promotion.infrastructure.persistence.adapter;

import com.cardapio.promotion.domain.model.Coupon;
import com.cardapio.promotion.domain.model.CouponCode;
import com.cardapio.promotion.domain.model.CouponId;
import com.cardapio.promotion.domain.port.CouponRepository;
import com.cardapio.promotion.infrastructure.persistence.jpa.CouponJpaEntity;
import com.cardapio.promotion.infrastructure.persistence.mapper.CouponMapper;
import com.cardapio.promotion.infrastructure.persistence.repository.SpringCouponJpaRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CouponRepositoryAdapter implements CouponRepository {

    private final SpringCouponJpaRepository jpa;

    @Override
    public void save(Coupon coupon) {
        var existing = jpa.findById(coupon.id().value());
        if (existing.isPresent()) {
            CouponMapper.update(existing.get(), coupon);
            jpa.save(existing.get());
        } else {
            jpa.save(CouponMapper.toJpa(coupon));
        }
    }

    @Override
    public Optional<Coupon> findById(CouponId id) {
        return jpa.findById(id.value()).map(CouponMapper::toDomain);
    }

    @Override
    public Optional<Coupon> findByCode(CouponCode code) {
        return jpa.findByCode(code.value()).map(CouponMapper::toDomain);
    }

    @Override
    public List<Coupon> findAll(boolean activeOnly, String codePrefix, int limit, int offset) {
        int page = offset / Math.max(1, limit);
        var pageable = PageRequest.of(page, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        return jpa.findAll(spec(activeOnly, codePrefix), pageable)
            .map(CouponMapper::toDomain)
            .toList();
    }

    @Override
    public long count(boolean activeOnly, String codePrefix) {
        return jpa.count(spec(activeOnly, codePrefix));
    }

    private Specification<CouponJpaEntity> spec(boolean activeOnly, String codePrefix) {
        return (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            if (activeOnly) preds.add(cb.isTrue(root.get("active")));
            if (codePrefix != null && !codePrefix.isBlank()) {
                preds.add(cb.like(root.get("code"), codePrefix.toUpperCase() + "%"));
            }
            return cb.and(preds.toArray(new Predicate[0]));
        };
    }
}
