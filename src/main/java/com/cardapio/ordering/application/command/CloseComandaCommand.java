package com.cardapio.ordering.application.command;

import com.cardapio.ordering.domain.model.ComandaId;

public record CloseComandaCommand(ComandaId comandaId) {}
