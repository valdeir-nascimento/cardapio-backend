package com.cardapio.ordering.api.dto;

import com.cardapio.ordering.application.command.AddCartItemCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

@Schema(description = "Adiciona um item ao carrinho do cliente. Suporta variação, adicionais e meia-meia (pizza).")
public record AddCartItemRequest(

    @Schema(description = "ID do produto.", example = "ad4f3c1b-7a02-4c41-bb9c-d56b9c92ec0a",
        format = "uuid", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull UUID productId,

    @Schema(description = "Variação escolhida (tamanho/formato). Null se o produto tem só uma opção.",
        example = "5fa6f72a-3b8e-4922-a6cb-882c0a32b2e6", format = "uuid", nullable = true)
    UUID variationId,

    @Schema(description = "Adicionais opcionais selecionados.", nullable = true)
    List<AddOnSelectionRequest> addOns,

    @Schema(description = "Meia-meia: ID do produto da metade esquerda. Use junto com `halfRightProductId`.",
        example = "ad4f3c1b-7a02-4c41-bb9c-d56b9c92ec0a", format = "uuid", nullable = true)
    UUID halfLeftProductId,

    @Schema(description = "Meia-meia: ID do produto da metade direita.",
        example = "ec3aa7d4-9b22-46f9-bd6f-50a17fae2c92", format = "uuid", nullable = true)
    UUID halfRightProductId,

    @Schema(description = "Observação para a cozinha (até 200 caracteres).",
        example = "Sem cebola, ponto da carne mal passada.", maxLength = 200, nullable = true)
    @Size(max = 200) String observation,

    @Schema(description = "Quantidade de unidades deste item.",
        example = "1", minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @Min(1) int quantity
) {
    public AddCartItemCommand toCommand(UUID customerId) {
        List<AddCartItemCommand.AddOnSelection> sel = addOns == null ? List.of()
            : addOns.stream().map(a -> new AddCartItemCommand.AddOnSelection(a.groupId(), a.itemId(), a.quantity())).toList();
        return new AddCartItemCommand(customerId, productId, variationId, sel,
            halfLeftProductId, halfRightProductId, observation, quantity);
    }

    @Schema(description = "Seleção de um adicional específico dentro de um grupo.")
    public record AddOnSelectionRequest(

        @Schema(description = "ID do grupo de adicionais.", format = "uuid",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull UUID groupId,

        @Schema(description = "ID do item dentro do grupo.", format = "uuid",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull UUID itemId,

        @Schema(description = "Quantidade do adicional (geralmente 1).", example = "1", minimum = "1")
        @Min(1) int quantity
    ) {}
}
