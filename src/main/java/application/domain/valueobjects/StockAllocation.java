package application.domain.valueobjects;

import application.domain.exceptions.common.InvalidDataException;
import java.util.Objects;

public final class StockAllocation {
    private final String productId;
    private final String warehouseId;
    private final int quantity;

    private StockAllocation(String productId, String warehouseId, int quantity) {
        this.productId = productId;
        this.warehouseId = warehouseId;
        this.quantity = quantity;
    }

    public static StockAllocation of(String productId, String warehouseId, int quantity) {
        if (productId == null || productId.isBlank()) {
            throw new InvalidDataException("Stock allocation product id is required");
        }
        if (warehouseId == null || warehouseId.isBlank()) {
            throw new InvalidDataException("Stock allocation warehouse id is required");
        }
        if (quantity <= 0) {
            throw new InvalidDataException("Stock allocation quantity must be greater than zero");
        }
        return new StockAllocation(productId.trim(), warehouseId.trim(), quantity);
    }

    public String getProductId() {
        return productId;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public int getQuantity() {
        return quantity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof StockAllocation that)) {
            return false;
        }
        return quantity == that.quantity
                && productId.equals(that.productId)
                && warehouseId.equals(that.warehouseId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, warehouseId, quantity);
    }

    @Override
    public String toString() {
        return productId + "@" + warehouseId + " x" + quantity;
    }
}
