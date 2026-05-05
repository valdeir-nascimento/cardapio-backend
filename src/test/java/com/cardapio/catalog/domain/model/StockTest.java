package com.cardapio.catalog.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockTest {

    @Test
    void untrackedStockHasNoQuantity() {
        Stock stock = Stock.untracked();
        assertThat(stock.isTracked()).isFalse();
        assertThat(stock.isInStock()).isTrue();
    }

    @Test
    void trackedZeroIsOutOfStock() {
        Stock stock = Stock.of(0);
        assertThat(stock.isTracked()).isTrue();
        assertThat(stock.isInStock()).isFalse();
        assertThat(stock.quantity()).isEqualTo(0);
    }

    @Test
    void trackedPositiveIsInStock() {
        Stock stock = Stock.of(5);
        assertThat(stock.isTracked()).isTrue();
        assertThat(stock.isInStock()).isTrue();
        assertThat(stock.quantity()).isEqualTo(5);
    }

    @Test
    void rejectsNegativeQuantity() {
        assertThatThrownBy(() -> Stock.of(-1))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void decrementReducesQuantity() {
        Stock stock = Stock.of(10).decrement(3);
        assertThat(stock.quantity()).isEqualTo(7);
    }

    @Test
    void decrementBelowZeroIsForbidden() {
        assertThatThrownBy(() -> Stock.of(2).decrement(5))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void decrementUntrackedReturnsUntracked() {
        Stock untracked = Stock.untracked();
        assertThat(untracked.decrement(10).isTracked()).isFalse();
    }
}
