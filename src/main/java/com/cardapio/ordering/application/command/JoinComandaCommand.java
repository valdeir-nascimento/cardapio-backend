package com.cardapio.ordering.application.command;

import com.cardapio.ordering.domain.model.ComandaId;

import java.util.UUID;

public record JoinComandaCommand(ComandaId comandaId, UUID customerId) {}
