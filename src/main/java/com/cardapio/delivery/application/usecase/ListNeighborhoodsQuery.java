package com.cardapio.delivery.application.usecase;

import com.cardapio.delivery.application.dto.NeighborhoodView;
import com.cardapio.delivery.domain.port.NeighborhoodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListNeighborhoodsQuery {

    private final NeighborhoodRepository repo;

    @Transactional(readOnly = true)
    public List<NeighborhoodView> all() {
        return repo.findAll().stream().map(NeighborhoodView::from).toList();
    }

    @Transactional(readOnly = true)
    public List<NeighborhoodView> active() {
        return repo.findAllActive().stream().map(NeighborhoodView::from).toList();
    }
}
