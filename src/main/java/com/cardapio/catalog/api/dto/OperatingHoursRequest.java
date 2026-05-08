package com.cardapio.catalog.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@Schema(description = "Horário de funcionamento por dia da semana. Cada dia pode ter múltiplas faixas (ex: almoço + jantar).")
public record OperatingHoursRequest(

    @Schema(
        description = "Mapa por dia da semana → lista de faixas. Dias omitidos = fechado.",
        example = """
            {
              "MONDAY": [{"openTime":"11:30","closeTime":"14:30"},{"openTime":"18:00","closeTime":"23:00"}],
              "FRIDAY": [{"openTime":"18:00","closeTime":"23:59"}],
              "SATURDAY": [{"openTime":"18:00","closeTime":"23:59"}]
            }
            """,
        requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull Map<DayOfWeek, List<TimeRangeDto>> hoursByDay
) {

    @Schema(description = "Faixa horária aberta-fechada do mesmo dia.")
    public record TimeRangeDto(

        @Schema(description = "Horário de abertura (HH:mm).", example = "11:30",
            requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull LocalTime openTime,

        @Schema(description = "Horário de fechamento (HH:mm). Deve ser maior que `openTime`.",
            example = "14:30", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull LocalTime closeTime
    ) {}
}
