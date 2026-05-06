package com.cardapio.delivery.application.usecase;

import com.cardapio.delivery.application.command.CreateNeighborhoodCommand;
import com.cardapio.delivery.application.command.UpdateNeighborhoodCommand;
import com.cardapio.delivery.domain.model.Neighborhood;
import com.cardapio.delivery.domain.model.NeighborhoodId;
import com.cardapio.delivery.domain.port.NeighborhoodRepository;
import com.cardapio.shared.domain.Money;
import com.cardapio.shared.domain.Result;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NeighborhoodUseCasesTest {

    private final NeighborhoodRepository repo = mock(NeighborhoodRepository.class);

    @Test
    void createValid() {
        Result<NeighborhoodId> r = new CreateNeighborhoodUseCase(repo).execute(
            new CreateNeighborhoodCommand("Centro", "Salvador", new BigDecimal("8.50")));
        assertThat(r.isSuccess()).isTrue();
        verify(repo).save(any());
    }

    @Test
    void createRejectsBlankName() {
        Result<NeighborhoodId> r = new CreateNeighborhoodUseCase(repo).execute(
            new CreateNeighborhoodCommand("  ", "Salvador", BigDecimal.TEN));
        assertThat(r.isSuccess()).isFalse();
    }

    @Test
    void createRejectsNegativeFee() {
        Result<NeighborhoodId> r = new CreateNeighborhoodUseCase(repo).execute(
            new CreateNeighborhoodCommand("Centro", "Salvador", new BigDecimal("-1")));
        assertThat(r.isSuccess()).isFalse();
    }

    @Test
    void createRejectsDuplicate() {
        when(repo.existsByNameAndCity("Centro", "Salvador")).thenReturn(true);
        Result<NeighborhoodId> r = new CreateNeighborhoodUseCase(repo).execute(
            new CreateNeighborhoodCommand("Centro", "Salvador", new BigDecimal("5")));
        assertThat(r.isSuccess()).isFalse();
        verify(repo, never()).save(any());
    }

    @Test
    void updateExisting() {
        NeighborhoodId id = NeighborhoodId.newId();
        Neighborhood existing = Neighborhood.rehydrate(id, "Old", "Salvador", Money.brl("5"), true);
        when(repo.findById(id)).thenReturn(Optional.of(existing));

        Result<NeighborhoodId> r = new UpdateNeighborhoodUseCase(repo).execute(
            new UpdateNeighborhoodCommand(id, "New", "Salvador", new BigDecimal("9.50"), false));

        assertThat(r.isSuccess()).isTrue();
        assertThat(existing.name()).isEqualTo("New");
        assertThat(existing.fee().amount()).isEqualByComparingTo(new BigDecimal("9.50"));
        assertThat(existing.isActive()).isFalse();
        verify(repo).save(existing);
    }

    @Test
    void updateMissingFails() {
        when(repo.findById(any())).thenReturn(Optional.empty());
        Result<NeighborhoodId> r = new UpdateNeighborhoodUseCase(repo).execute(
            new UpdateNeighborhoodCommand(NeighborhoodId.newId(), "X", "Y", BigDecimal.ONE, true));
        assertThat(r.isSuccess()).isFalse();
    }

    @Test
    void deleteExisting() {
        NeighborhoodId id = NeighborhoodId.newId();
        when(repo.existsById(id)).thenReturn(true);
        Result<Void> r = new DeleteNeighborhoodUseCase(repo).execute(id);
        assertThat(r.isSuccess()).isTrue();
        verify(repo).deleteById(id);
    }

    @Test
    void deleteMissingFails() {
        NeighborhoodId id = NeighborhoodId.newId();
        when(repo.existsById(id)).thenReturn(false);
        Result<Void> r = new DeleteNeighborhoodUseCase(repo).execute(id);
        assertThat(r.isSuccess()).isFalse();
        verify(repo, never()).deleteById(any());
    }
}
