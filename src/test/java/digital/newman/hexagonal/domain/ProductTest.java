package digital.newman.hexagonal.domain;

import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.*;

public class ProductTest {
    private static final Currency USD = Currency.getInstance("USD");

    @Test
    void shouldCreateProductWithZeroStock() {
        // Arrange
        var amount = new BigDecimal("100.00");
        var price = new Money(amount, USD);

        // Act
        var product = Product.create("some_name", "some_description", price);

        // Assert
        assertThat(product.getId()).isNotNull();
        assertThat(product.getName()).isEqualTo("some_name");
        assertThat(product.getDescription()).isEqualTo("some_description");
        assertThat(product.getPrice()).isEqualTo(price);
        assertThat(product.getStock()).isEqualTo(0);
    }
}
