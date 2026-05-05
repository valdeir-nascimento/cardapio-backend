package com.cardapio.identity.application.dto;

import com.cardapio.identity.domain.model.CustomerId;

public record CustomerProfile(CustomerId id, String name, String email, String phoneNumber) {}
