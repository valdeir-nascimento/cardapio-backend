package com.cardapio.ordering.domain.model;

import com.cardapio.shared.domain.Money;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValueObjectsTest {

    @Test
    void deliveryAddressRejectsBlanks() {
        assertThatThrownBy(() -> new DeliveryAddress("", "10", null, "Centro", "SSA", "40000-000", UUID.randomUUID()))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void selectedAddOnRequiresPositiveQuantity() {
        assertThatThrownBy(() -> new SelectedAddOn(UUID.randomUUID(), UUID.randomUUID(), "Bacon", Money.brl("1"), 0))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void halfAndHalfRequiresDistinctProducts() {
        UUID p = UUID.randomUUID();
        assertThatThrownBy(() -> new HalfAndHalf(p, p, "x", Money.brl("10")))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void observationTrimsAndCapsLength() {
        assertThat(Observation.of("  hello  ").value()).isEqualTo("hello");
        assertThat(Observation.empty().isEmpty()).isTrue();
        assertThatThrownBy(() -> new Observation("x".repeat(201)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void observationOfNullReturnsEmpty() {
        assertThat(Observation.of(null).isEmpty()).isTrue();
    }
}
