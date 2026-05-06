package com.cardapio.ordering.infrastructure.persistence.adapter;

import com.cardapio.ordering.domain.model.Table;
import com.cardapio.ordering.domain.model.TableId;
import com.cardapio.ordering.domain.port.TableRepository;
import com.cardapio.ordering.infrastructure.persistence.mapper.TableMapper;
import com.cardapio.ordering.infrastructure.persistence.repository.SpringTableJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TableRepositoryAdapter implements TableRepository {

    private final SpringTableJpaRepository jpa;

    @Override
    public void save(Table table) {
        var existing = jpa.findById(table.id().value());
        if (existing.isPresent()) {
            TableMapper.update(existing.get(), table);
            jpa.save(existing.get());
        } else {
            jpa.save(TableMapper.toJpa(table));
        }
    }

    @Override
    public Optional<Table> findById(TableId id) {
        return jpa.findById(id.value()).map(TableMapper::toDomain);
    }

    @Override
    public Optional<Table> findByQrToken(UUID qrToken) {
        return jpa.findByQrToken(qrToken).map(TableMapper::toDomain);
    }

    @Override
    public Optional<Table> findByNumber(int number) {
        return jpa.findByNumber(number).map(TableMapper::toDomain);
    }

    @Override
    public List<Table> findAllOrderByNumber() {
        return jpa.findAllByOrderByNumberAsc().stream().map(TableMapper::toDomain).toList();
    }
}
