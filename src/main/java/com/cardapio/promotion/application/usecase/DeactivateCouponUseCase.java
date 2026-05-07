package com.cardapio.promotion.application.usecase;

import com.cardapio.promotion.application.dto.CouponView;
import com.cardapio.promotion.domain.model.Coupon;
import com.cardapio.promotion.domain.model.CouponId;
import com.cardapio.promotion.domain.port.CouponRepository;
import com.cardapio.shared.domain.ErrorCode;
import com.cardapio.shared.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@RequiredArgsConstructor
public class DeactivateCouponUseCase {

    private final CouponRepository coupons;
    private final Clock clock;

    @Transactional
    public Result<CouponView> execute(CouponId id) {
        Coupon coupon = coupons.findById(id).orElse(null);
        if (coupon == null) return Result.failWith(ErrorCode.COUPON_NOT_FOUND);
        coupon.deactivate(clock);
        coupons.save(coupon);
        return Result.success(CouponView.from(coupon));
    }
}
