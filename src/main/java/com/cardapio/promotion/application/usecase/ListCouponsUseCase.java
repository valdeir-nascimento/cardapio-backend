package com.cardapio.promotion.application.usecase;

import com.cardapio.promotion.application.dto.CouponView;
import com.cardapio.promotion.domain.port.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListCouponsUseCase {

    private final CouponRepository coupons;

    @Transactional(readOnly = true)
    public List<CouponView> execute(boolean activeOnly, String codePrefix, int limit, int offset) {
        return coupons.findAll(activeOnly, codePrefix, limit, offset).stream()
            .map(CouponView::from).toList();
    }

    @Transactional(readOnly = true)
    public long count(boolean activeOnly, String codePrefix) {
        return coupons.count(activeOnly, codePrefix);
    }
}
