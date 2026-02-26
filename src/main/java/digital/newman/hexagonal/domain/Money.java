package digital.newman.hexagonal.domain;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

public record Money(BigDecimal amount, Currency currency) {

    /**
     * Compact constructor that validates the Money record.
     * Ensures that:
     * - amount is not null
     * - currency is not null
     * - amount is non-negative (>= 0)
     *
     * @throws IllegalArgumentException if amount is null, currency is null, or amount is negative
     */
    public Money {
        Objects.requireNonNull(amount, "Amount cannot be null");
        Objects.requireNonNull(currency, "Currency cannot be null");
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("Amount must be non-negative, but got: " + amount);
        }
    }
}
