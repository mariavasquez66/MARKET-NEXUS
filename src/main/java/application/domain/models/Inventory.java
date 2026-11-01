package application.domain.models;

import application.domain.enums.InventoryCondition;
import application.domain.exceptions.common.InvalidDataException;
import application.domain.exceptions.inventory.DamagedInventoryException;
import application.domain.exceptions.inventory.InsufficientStockException;
import application.domain.exceptions.inventory.InvalidReservationException;
import application.domain.exceptions.inventory.NegativeStockException;
import application.domain.exceptions.product.InvalidProductTypeException;
import java.util.Objects;
import java.util.UUID;

public class Inventory {
    private final String id;
    private final Product product;
    private final Warehouse warehouse;
    private int availableQuantity;
    private int reservedQuantity;
    private InventoryCondition condition;

    private Inventory(String id,
                      Product product,
                      Warehouse warehouse,
                      int availableQuantity,
                      int reservedQuantity,
                      InventoryCondition condition) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.product = Objects.requireNonNull(product, "product cannot be null");
        this.warehouse = Objects.requireNonNull(warehouse, "warehouse cannot be null");
        if (availableQuantity < 0) {
            throw new NegativeStockException("Available quantity cannot be negative");
        }
        if (reservedQuantity < 0) {
            throw new NegativeStockException("Reserved quantity cannot be negative");
        }
        this.availableQuantity = availableQuantity;
        this.reservedQuantity = reservedQuantity;
        this.condition = Objects.requireNonNull(condition, "condition cannot be null");
    }

    public static Inventory create(Product product, Warehouse warehouse, int initialQuantity) {
        if (product == null || !product.requiresInventory()) {
            throw new InvalidProductTypeException("Inventory is only allowed for physical products");
        }
        if (initialQuantity < 0) {
            throw new NegativeStockException("Initial quantity cannot be negative");
        }
        return new Inventory(UUID.randomUUID().toString(), product, warehouse,
                initialQuantity, 0, InventoryCondition.GOOD);
    }

    public static Inventory reconstitute(String id,
                                         Product product,
                                         Warehouse warehouse,
                                         int availableQuantity,
                                         int reservedQuantity,
                                         InventoryCondition condition) {
        return new Inventory(id, product, warehouse, availableQuantity, reservedQuantity, condition);
    }

    public void reserve(int quantity) {
        assertGoodCondition();
        assertPositive(quantity);
        if (quantity > availableQuantity) {
            throw new InsufficientStockException("Insufficient available stock");
        }
        availableQuantity -= quantity;
        reservedQuantity += quantity;
    }

    public void releaseReservation(int quantity) {
        assertPositive(quantity);
        if (quantity > reservedQuantity) {
            throw new InvalidReservationException("Cannot release more than reserved");
        }
        reservedQuantity -= quantity;
        availableQuantity += quantity;
    }

    public void confirmSale(int quantity) {
        assertGoodCondition();
        assertPositive(quantity);
        if (quantity > reservedQuantity) {
            throw new InvalidReservationException("Cannot confirm more than reserved");
        }
        reservedQuantity -= quantity;
    }

    public void adjust(int delta) {
        if (delta == 0) {
            throw new InvalidDataException("Adjustment delta cannot be zero");
        }
        if (availableQuantity + delta < 0) {
            throw new NegativeStockException("Adjustment would leave negative stock");
        }
        availableQuantity += delta;
    }

    public void registerReturn(int quantity) {
        assertGoodCondition();
        assertPositive(quantity);
        availableQuantity += quantity;
    }

    public void markDamaged() {
        if (!condition.equals(InventoryCondition.GOOD)) {
            throw new DamagedInventoryException("Inventory is already damaged");
        }
        if (reservedQuantity != 0) {
            throw new InvalidReservationException("Cannot mark damaged inventory with reservations");
        }
        condition = InventoryCondition.DAMAGED;
    }

    public void markRepaired() {
        if (!condition.equals(InventoryCondition.DAMAGED)) {
            throw new InvalidDataException("Inventory is not damaged");
        }
        condition = InventoryCondition.GOOD;
    }

    private void assertGoodCondition() {
        if (!condition.equals(InventoryCondition.GOOD)) {
            throw new DamagedInventoryException("Damaged inventory cannot be reserved or sold");
        }
    }

    private static void assertPositive(int quantity) {
        if (quantity <= 0) {
            throw new InvalidReservationException("Quantity must be greater than zero");
        }
    }

    public String getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public Warehouse getWarehouse() {
        return warehouse;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public int getReservedQuantity() {
        return reservedQuantity;
    }

    public InventoryCondition getCondition() {
        return condition;
    }
}
