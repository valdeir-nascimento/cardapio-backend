package com.cardapio.ordering.domain.port;

import com.cardapio.ordering.domain.model.Comanda;
import com.cardapio.ordering.domain.model.ComandaId;
import com.cardapio.ordering.domain.model.ComandaStatus;
import com.cardapio.ordering.domain.model.TableId;

import java.util.List;
import java.util.Optional;

public interface ComandaRepository {
    void save(Comanda comanda);
    Optional<Comanda> findById(ComandaId id);
    Optional<Comanda> findOpenByTableId(TableId tableId);
    List<Comanda> findByStatus(ComandaStatus status);
}
