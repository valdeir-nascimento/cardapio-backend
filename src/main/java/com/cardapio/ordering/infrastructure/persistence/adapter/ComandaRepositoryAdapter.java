package com.cardapio.ordering.infrastructure.persistence.adapter;

import com.cardapio.ordering.domain.model.Comanda;
import com.cardapio.ordering.domain.model.ComandaId;
import com.cardapio.ordering.domain.model.ComandaStatus;
import com.cardapio.ordering.domain.model.TableId;
import com.cardapio.ordering.domain.port.ComandaRepository;
import com.cardapio.ordering.infrastructure.persistence.mapper.ComandaMapper;
import com.cardapio.ordering.infrastructure.persistence.repository.SpringComandaJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ComandaRepositoryAdapter implements ComandaRepository {

    private final SpringComandaJpaRepository jpa;

    @Override
    public void save(Comanda comanda) {
        var existing = jpa.findById(comanda.id().value());
        if (existing.isPresent()) {
            ComandaMapper.update(existing.get(), comanda);
            jpa.save(existing.get());
        } else {
            jpa.save(ComandaMapper.toJpa(comanda));
        }
    }

    @Override
    public Optional<Comanda> findById(ComandaId id) {
        return jpa.findById(id.value()).map(ComandaMapper::toDomain);
    }

    @Override
    public Optional<Comanda> findOpenByTableId(TableId tableId) {
        return jpa.findFirstByTableIdAndStatus(tableId.value(), ComandaStatus.OPEN.name())
            .map(ComandaMapper::toDomain);
    }

    @Override
    public List<Comanda> findByStatus(ComandaStatus status) {
        return jpa.findAllByStatusOrderByOpenedAtDesc(status.name())
            .stream().map(ComandaMapper::toDomain).toList();
    }
}
