package com.cardapio.ordering.application.command;

import com.cardapio.ordering.domain.model.TableId;

import java.util.UUID;

public record OpenComandaCommand(TableId tableId, UUID openerCustomerId) {}
