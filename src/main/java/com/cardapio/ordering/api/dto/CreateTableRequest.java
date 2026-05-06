package com.cardapio.ordering.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateTableRequest(@NotNull @Min(1) Integer number) {}
