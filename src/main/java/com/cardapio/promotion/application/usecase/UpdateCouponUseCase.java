package com.cardapio.promotion.application.usecase;

import com.cardapio.promotion.application.command.UpdateCouponCommand;
import com.cardapio.promotion.application.dto.CouponView;
import com.cardapio.promotion.domain.exception.CouponInvariantException;
import com.cardapio.promotion.domain.model.Coupon;
import com.cardapio.promotion.domain.port.CouponRepository;
import com.cardapio.shared.domain.ErrorCode;
import com.cardapio.shared.domain.Money;
import com.cardapio.shared.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@RequiredArgsConstructor
public class UpdateCouponUseCase {

    private final CouponRepository coupons;
    private final Clock clock;

    @Transactional
    public Result<CouponView> execute(UpdateCouponCommand cmd) {
        Coupon coupon = coupons.findById(cmd.id()).orElse(null);
        if (coupon == null) return Result.failWith(ErrorCode.COUPON_NOT_FOUND);
        Money minOrder = cmd.minOrderValue() == null ? null
            : Money.of(cmd.minOrderValue(), coupon.currency());
        try {
            coupon.update(cmd.value(), cmd.validUntil(), minOrder, cmd.maxUses(), clock);
            coupons.save(coupon);
            return Result.success(CouponView.from(coupon));
        } catch (CouponInvariantException e) {
            return Result.failWith(ErrorCode.INVALID_COUPON, e.getMessage());
        }
    }
}
