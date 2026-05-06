package com.cardapio.ordering.domain.port;

import com.cardapio.ordering.domain.model.Table;
import com.cardapio.ordering.domain.model.TableId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TableRepository {
    void save(Table table);
    Optional<Table> findById(TableId id);
    Optional<Table> findByQrToken(UUID qrToken);
    Optional<Table> findByNumber(int number);
    List<Table> findAllOrderByNumber();
}
