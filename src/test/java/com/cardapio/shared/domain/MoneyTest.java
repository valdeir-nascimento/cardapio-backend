package com.cardapio.shared.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    private static final Currency BRL = Currency.getInstance("BRL");

    @Test
    void createsBrlAmountFromString() {
        Money money = Money.brl("19.90");
        assertThat(money.amount()).isEqualByComparingTo("19.90");
        assertThat(money.currency()).isEqualTo(BRL);
    }

    @Test
    void rejectsNegativeAmounts() {
        assertThatThrownBy(() -> Money.brl("-1.00"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("non-negative");
    }

    @Test
    void rejectsNullAmount() {
        assertThatThrownBy(() -> Money.of(null, BRL))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void scalesAmountToTwoDecimals() {
        Money money = Money.brl("10");
        assertThat(money.amount().scale()).isEqualTo(2);
        assertThat(money.amount()).isEqualByComparingTo("10.00");
    }

    @Test
    void addsTwoAmountsOfSameCurrency() {
        Money a = Money.brl("5.00");
        Money b = Money.brl("3.50");
        assertThat(a.add(b).amount()).isEqualByComparingTo("8.50");
    }

    @Test
    void rejectsAddingDifferentCurrencies() {
        Money brl = Money.brl("1.00");
        Money usd = Money.of(new BigDecimal("1.00"), Currency.getInstance("USD"));
        assertThatThrownBy(() -> brl.add(usd))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("currency");
    }

    @Test
    void multipliesByPositiveInteger() {
        Money price = Money.brl("12.50");
        assertThat(price.multiply(3).amount()).isEqualByComparingTo("37.50");
    }

    @Test
    void rejectsMultiplyByNegative() {
        Money price = Money.brl("12.50");
        assertThatThrownBy(() -> price.multiply(-1))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void subtractsAmountsOfSameCurrency() {
        Money a = Money.brl("10.00");
        Money b = Money.brl("3.00");
        assertThat(a.subtract(b).amount()).isEqualByComparingTo("7.00");
    }

    @Test
    void rejectsSubtractionLeadingToNegative() {
        Money a = Money.brl("3.00");
        Money b = Money.brl("10.00");
        assertThatThrownBy(() -> a.subtract(b))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("non-negative");
    }

    @Test
    void zeroIsUsableConstant() {
        assertThat(Money.zeroBrl().amount()).isEqualByComparingTo("0.00");
    }

    @Test
    void equalsAndHashCodeBasedOnAmountAndCurrency() {
        Money a = Money.brl("5.00");
        Money b = Money.brl("5.00");
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }
}
