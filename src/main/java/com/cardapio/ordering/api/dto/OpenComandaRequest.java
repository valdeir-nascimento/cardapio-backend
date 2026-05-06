package com.cardapio.ordering.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record OpenComandaRequest(@NotNull UUID tableId) {}
