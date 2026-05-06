package com.cardapio.promotion.application.usecase;

import com.cardapio.promotion.application.command.CreateCouponCommand;
import com.cardapio.promotion.application.dto.CouponView;
import com.cardapio.promotion.domain.exception.CouponInvariantException;
import com.cardapio.promotion.domain.model.Coupon;
import com.cardapio.promotion.domain.model.CouponCode;
import com.cardapio.promotion.domain.port.CouponRepository;
import com.cardapio.shared.domain.ErrorCode;
import com.cardapio.shared.domain.Money;
import com.cardapio.shared.domain.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Currency;

@Service
@RequiredArgsConstructor
public class CreateCouponUseCase {

    private final CouponRepository coupons;
    private final Clock clock;

    @Transactional
    public Result<CouponView> execute(CreateCouponCommand cmd) {
        CouponCode code;
        try {
            code = CouponCode.of(cmd.code());
        } catch (IllegalArgumentException e) {
            return Result.failWith(ErrorCode.INVALID_COUPON, e.getMessage());
        }
        if (coupons.findByCode(code).isPresent()) {
            return Result.failWith(ErrorCode.COUPON_CODE_TAKEN);
        }
        Currency currency = Currency.getInstance(cmd.currency());
        Money minOrder = cmd.minOrderValue() == null ? null : Money.of(cmd.minOrderValue(), currency);
        try {
            Coupon coupon = Coupon.create(code, cmd.type(), cmd.value(), currency,
                cmd.validFrom(), cmd.validUntil(), minOrder, cmd.maxUses(), clock);
            coupons.save(coupon);
            return Result.success(CouponView.from(coupon));
        } catch (CouponInvariantException e) {
            return Result.failWith(ErrorCode.INVALID_COUPON, e.getMessage());
        }
    }
}
