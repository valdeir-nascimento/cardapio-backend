package com.cardapio.catalog.api.dto;

import java.util.UUID;

public record CategoryResponse(UUID id, String name, int displayOrder, boolean active) {}
