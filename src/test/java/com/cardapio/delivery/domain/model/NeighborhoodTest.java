package com.cardapio.delivery.domain.model;

import com.cardapio.shared.domain.Money;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NeighborhoodTest {

    @Test
    void createsActiveByDefault() {
        Neighborhood n = Neighborhood.create("Centro", "Salvador", Money.brl("8.50"));
        assertThat(n.isActive()).isTrue();
        assertThat(n.name()).isEqualTo("Centro");
        assertThat(n.city()).isEqualTo("Salvador");
        assertThat(n.fee()).isEqualTo(Money.brl("8.50"));
    }

    @Test
    void rejectsBlankName() {
        assertThatThrownBy(() -> Neighborhood.create("  ", "Salvador", Money.brl("5")))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsBlankCity() {
        assertThatThrownBy(() -> Neighborhood.create("Centro", " ", Money.brl("5")))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNegativeFeeViaMoney() {
        assertThatThrownBy(() -> Money.brl("-1"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deactivateFlipsFlag() {
        Neighborhood n = Neighborhood.create("Pituba", "Salvador", Money.brl("10.00"));
        n.deactivate();
        assertThat(n.isActive()).isFalse();
        n.activate();
        assertThat(n.isActive()).isTrue();
    }

    @Test
    void changeFeeUpdatesValue() {
        Neighborhood n = Neighborhood.create("Barra", "Salvador", Money.brl("12.00"));
        n.changeFee(Money.brl("15.00"));
        assertThat(n.fee()).isEqualTo(Money.brl("15.00"));
    }
}
