package com.cardapio.ordering.api.dto;

import com.cardapio.ordering.application.command.PlaceOrderCommand;
import com.cardapio.ordering.domain.model.ComandaId;
import com.cardapio.ordering.domain.model.OrderModality;
import com.cardapio.ordering.domain.model.TableId;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(description = "Fecha o carrinho como pedido. Os itens são lidos do carrinho ativo do cliente.")
public record PlaceOrderRequest(

    @Schema(description = "Como o pedido é entregue. `DELIVERY` exige `address`. `DINE_IN` exige `tableId` ou `comandaId`. `PICKUP` não exige nada.",
        example = "DELIVERY", requiredMode = Schema.RequiredMode.REQUIRED,
        allowableValues = {"DELIVERY", "DINE_IN", "PICKUP"})
    @NotNull OrderModality modality,

    @Schema(description = "Endereço de entrega. Obrigatório quando `modality=DELIVERY`.", nullable = true)
    @Valid AddressRequest address,

    @Schema(description = "Mesa do pedido (apenas DINE_IN avulso, sem comanda).",
        example = "f12a3c8e-1b4d-4e9c-90b1-aa7b3e2c45ab", format = "uuid", nullable = true)
    UUID tableId,

    @Schema(description = "Comanda à qual o pedido pertence (DINE_IN com comanda compartilhada).",
        example = "07e2b8d8-cc88-4d4d-9311-ec0a0c6a2af0", format = "uuid", nullable = true)
    UUID comandaId
) {
    public PlaceOrderCommand toCommand(UUID customerId, String idempotencyKey) {
        PlaceOrderCommand.DeliveryAddressInput a = address == null ? null
            : new PlaceOrderCommand.DeliveryAddressInput(
                address.street(), address.number(), address.complement(),
                address.district(), address.city(), address.postalCode(), address.neighborhoodId());
        return new PlaceOrderCommand(
            customerId,
            modality,
            a,
            tableId == null ? null : TableId.of(tableId),
            comandaId == null ? null : ComandaId.of(comandaId),
            idempotencyKey);
    }

    @Schema(description = "Endereço completo de entrega.")
    public record AddressRequest(
        @Schema(example = "Rua das Flores", maxLength = 160, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 160) String street,

        @Schema(example = "123", maxLength = 20, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 20) String number,

        @Schema(example = "Apto 42, Bloco B", maxLength = 120, nullable = true)
        @Size(max = 120) String complement,

        @Schema(example = "Vila Mariana", maxLength = 120, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 120) String district,

        @Schema(example = "São Paulo", maxLength = 120, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 120) String city,

        @Schema(description = "CEP no formato 00000-000.", example = "04101-300",
            maxLength = 16, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 16) String postalCode,

        @Schema(description = "ID do bairro cadastrado em `/api/v1/delivery/neighborhoods`. A taxa de entrega vem dele.",
            example = "8b4e3d2a-c1f5-4321-9876-fedcba012345", format = "uuid",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull UUID neighborhoodId
    ) {}
}
