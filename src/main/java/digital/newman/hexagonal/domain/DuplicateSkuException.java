package digital.newman.hexagonal.domain;

public class DuplicateSkuException extends RuntimeException {
    public DuplicateSkuException(String sku) {
        super("Duplicate SKU: " + sku);
    }
}
