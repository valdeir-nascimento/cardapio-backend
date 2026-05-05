package com.cardapio.identity.api.dto;

import com.cardapio.identity.application.dto.CustomerProfile;

import java.util.UUID;

public record ProfileResponse(UUID id, String name, String email, String phoneNumber) {
    public static ProfileResponse from(CustomerProfile p) {
        return new ProfileResponse(p.id().value(), p.name(), p.email(), p.phoneNumber());
    }
}
