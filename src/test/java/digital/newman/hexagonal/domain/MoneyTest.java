package digital.newman.hexagonal.domain;

import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.*;

class MoneyTest {
    private static final Currency USD = Currency.getInstance("USD");

    @Test
    void shouldCreateMoneyWithPositiveAmount() {
        // Arrange
        var amount = new BigDecimal("100.00");

        // Act
        var money = new Money(amount, USD);

        // Assert
        assertThat(money.amount()).isEqualTo(amount);
        assertThat(money.currency()).isEqualTo(USD);
    }

    @Test
    void shouldCreateMoneyWithZeroAmount() {
        // Arrange
        var amount = BigDecimal.ZERO;

        // Act
        var money = new Money(amount, USD);

        // Assert
        assertThat(money.amount()).isEqualTo(amount);
        assertThat(money.currency()).isEqualTo(USD);
    }

    @Test
    void shouldThrowExceptionForNegativeAmount() {
        // Arrange
        var negativeAmount = new BigDecimal("-50.00");

        // Act
        ThrowableAssert.ThrowingCallable actor = () -> new Money(negativeAmount, USD);

        // Assert
        assertThatThrownBy(actor)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Amount must be non-negative, but got: -50.00");
    }

    @Test
    void shouldThrowExceptionForNullAmount() {
        // Act
        ThrowableAssert.ThrowingCallable actor = () -> new Money(null, USD);

        // Assert
        assertThatThrownBy(actor)
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Amount cannot be null");
    }

    @Test
    void shouldThrowExceptionForNullCurrency() {
        // Arrange
        var amount = new BigDecimal("100.00");

        // Act
        ThrowableAssert.ThrowingCallable actor = () -> new Money(amount, null);

        // Assert
        assertThatThrownBy(actor)
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Currency cannot be null");
    }
}

