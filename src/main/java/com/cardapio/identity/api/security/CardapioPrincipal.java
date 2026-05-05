package com.cardapio.identity.api.security;

import com.cardapio.identity.domain.model.Audience;
import com.cardapio.identity.domain.model.Role;

import java.util.Set;
import java.util.UUID;

public record CardapioPrincipal(UUID subject, Audience audience, Set<Role> roles) {}
