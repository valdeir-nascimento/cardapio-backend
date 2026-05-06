package com.cardapio.promotion.api.rest;

import com.cardapio.api.error.NotFoundException;
import com.cardapio.api.error.NotificationException;
import com.cardapio.promotion.api.dto.CouponResponse;
import com.cardapio.promotion.api.dto.CreateCouponRequest;
import com.cardapio.promotion.api.dto.UpdateCouponRequest;
import com.cardapio.promotion.application.command.UpdateCouponCommand;
import com.cardapio.promotion.application.usecase.CreateCouponUseCase;
import com.cardapio.promotion.application.usecase.DeactivateCouponUseCase;
import com.cardapio.promotion.application.usecase.ListCouponsUseCase;
import com.cardapio.promotion.application.usecase.UpdateCouponUseCase;
import com.cardapio.promotion.domain.model.CouponId;
import com.cardapio.shared.domain.Notification;
import com.cardapio.shared.domain.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/coupons")
@PreAuthorize("hasAnyRole('OWNER','MANAGER')")
public class AdminCouponController {

    private final CreateCouponUseCase createCoupon;
    private final UpdateCouponUseCase updateCoupon;
    private final DeactivateCouponUseCase deactivateCoupon;
    private final ListCouponsUseCase listCoupons;

    @PostMapping
    public ResponseEntity<CouponResponse> create(@Valid @RequestBody CreateCouponRequest body) {
        var view = unwrap(createCoupon.execute(body.toCommand()));
        return ResponseEntity
            .created(URI.create("/api/v1/admin/coupons/" + view.id().value()))
            .body(CouponResponse.from(view));
    }

    @PatchMapping("/{id}")
    public CouponResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateCouponRequest body) {
        var cmd = new UpdateCouponCommand(CouponId.of(id),
            body.value(), body.validUntil(), body.minOrderValue(), body.maxUses());
        return CouponResponse.from(unwrap(updateCoupon.execute(cmd)));
    }

    @PostMapping("/{id}/deactivate")
    public CouponResponse deactivate(@PathVariable UUID id) {
        return CouponResponse.from(unwrap(deactivateCoupon.execute(CouponId.of(id))));
    }

    @GetMapping
    public List<CouponResponse> list(
        @RequestParam(defaultValue = "false") boolean activeOnly,
        @RequestParam(required = false) String code,
        @RequestParam(defaultValue = "20") int limit,
        @RequestParam(defaultValue = "0") int offset
    ) {
        return listCoupons.execute(activeOnly, code, limit, offset).stream()
            .map(CouponResponse::from).toList();
    }

    private static <T> T unwrap(Result<T> r) {
        return r.orElseThrow(AdminCouponController::toException);
    }

    private static RuntimeException toException(Notification n) {
        boolean notFound = n.errors().stream()
            .anyMatch(e -> e.code() != null && e.code().endsWith("_NOT_FOUND"));
        return notFound ? new NotFoundException(n) : new NotificationException(n);
    }
}
