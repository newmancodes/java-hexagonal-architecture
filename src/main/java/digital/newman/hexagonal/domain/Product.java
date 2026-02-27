package digital.newman.hexagonal.domain;

import lombok.Getter;

public class Product {
    @Getter
    private final ProductId id;

    @Getter
    private String name;

    @Getter
    private String description;

    @Getter
    private Money price;

    @Getter
    private int stock;

    protected Product(ProductId id, String name, String description, Money price) {
        this(id, name, description, price, 0);
    }

    protected Product(ProductId id, String name, String description, Money price, int stock) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.stock = stock;
    }

    public void adjustStock(int delta) {
        if (this.stock + delta < 0) {
            throw new InsufficientStockException("Insufficient stock for product: " + this.getId());
        }

        this.stock += delta;
    }

    public void updateDetails(String name, String description, Money price) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Product name cannot be blank");
        }

        this.name = name;
        this.description = description;
        this.price = price;
    }

    public static Product create(String name, String description, Money price) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Product name cannot be blank");
        }

        return new Product(ProductId.generate(), name, description, price);
    }

    public static Product reconstitute(ProductId id, String name, String description, Money price, int stock) {
        return new Product(id, name, description, price, stock);
    }
}
