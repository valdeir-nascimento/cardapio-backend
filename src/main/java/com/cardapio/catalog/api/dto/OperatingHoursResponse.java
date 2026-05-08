package com.cardapio.catalog.api.dto;

import com.cardapio.catalog.application.dto.OperatingHoursView;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Schema(description = "Horário público de funcionamento. Dias ausentes do mapa significam que o estabelecimento está fechado.")
public record OperatingHoursResponse(

    @Schema(description = "Mapa por dia da semana → faixas de funcionamento.",
        example = """
            {
              "MONDAY": [{"openTime":"11:30","closeTime":"14:30"},{"openTime":"18:00","closeTime":"23:00"}],
              "TUESDAY": [{"openTime":"18:00","closeTime":"23:00"}]
            }
            """)
    Map<DayOfWeek, List<TimeRangeDto>> hoursByDay
) {

    @Schema(description = "Faixa de horário no formato HH:mm.")
    public record TimeRangeDto(
        @Schema(example = "11:30") LocalTime openTime,
        @Schema(example = "14:30") LocalTime closeTime
    ) {}

    public static OperatingHoursResponse from(OperatingHoursView v) {
        return new OperatingHoursResponse(v.hoursByDay().entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey,
                e -> e.getValue().stream().map(t -> new TimeRangeDto(t.openTime(), t.closeTime())).toList())));
    }
}
