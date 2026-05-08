package com.cardapio.delivery.api.rest;

import com.cardapio.api.error.NotFoundException;
import com.cardapio.delivery.api.dto.NeighborhoodResponse;
import com.cardapio.delivery.application.DeliveryFacade;
import com.cardapio.delivery.domain.model.NeighborhoodId;
import com.cardapio.shared.domain.ErrorCode;
import com.cardapio.shared.domain.Notification;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/delivery")
public class PublicDeliveryController implements PublicDeliveryApi {

    private final DeliveryFacade delivery;

    @Override
    @GetMapping("/neighborhoods")
    public List<NeighborhoodResponse> active() {
        return delivery.listActive().stream().map(NeighborhoodResponse::from).toList();
    }

    @Override
    @GetMapping("/fee")
    public DeliveryFeeResponse fee(@RequestParam UUID neighborhoodId) {
        return delivery.getActiveFee(NeighborhoodId.of(neighborhoodId))
            .map(m -> new DeliveryFeeResponse(m.amount(), m.currency().getCurrencyCode()))
            .orElseThrow(() -> new NotFoundException(Notification.ofSingle(ErrorCode.NEIGHBORHOOD_NOT_SERVED)));
    }

    @Schema(description = "Taxa de entrega aplicável.")
    public record DeliveryFeeResponse(
        @Schema(description = "Valor em reais.", example = "10.00") BigDecimal fee,
        @Schema(example = "BRL") String currency
    ) {}
}
