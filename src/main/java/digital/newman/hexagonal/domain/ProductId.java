package digital.newman.hexagonal.domain;

import java.util.UUID;

public record ProductId(UUID value) {
    public static ProductId generate() {
        return new ProductId(UUID.randomUUID());
    }

    public static ProductId of(UUID value) {
        return new ProductId(value);
    }
}
