package application.domain.models;

import application.domain.enums.MovementType;
import application.domain.exceptions.common.InvalidDataException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class InventoryMovement {
    private final String id;
    private final Inventory inventory;
    private final MovementType type;
    private final int quantity;
    private final User performedBy;
    private final Instant occurredAt;

    private InventoryMovement(String id,
                              Inventory inventory,
                              MovementType type,
                              int quantity,
                              User performedBy,
                              Instant occurredAt) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.inventory = Objects.requireNonNull(inventory, "inventory cannot be null");
        this.type = Objects.requireNonNull(type, "type cannot be null");
        if (quantity == 0) {
            throw new InvalidDataException("Movement quantity cannot be zero");
        }
        if (!type.equals(MovementType.ADJUSTMENT) && quantity < 0) {
            throw new InvalidDataException("Only adjustments may have negative quantity");
        }
        this.quantity = quantity;
        this.performedBy = Objects.requireNonNull(performedBy, "performedBy cannot be null");
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
    }

    public static InventoryMovement create(Inventory inventory,
                                           MovementType type,
                                           int quantity,
                                           User performedBy,
                                           Instant occurredAt) {
        return new InventoryMovement(UUID.randomUUID().toString(), inventory, type,
                quantity, performedBy, occurredAt);
    }

    public static InventoryMovement reconstitute(String id,
                                                 Inventory inventory,
                                                 MovementType type,
                                                 int quantity,
                                                 User performedBy,
                                                 Instant occurredAt) {
        return new InventoryMovement(id, inventory, type, quantity, performedBy, occurredAt);
    }

    public String getId() {
        return id;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public MovementType getType() {
        return type;
    }

    public int getQuantity() {
        return quantity;
    }

    public User getPerformedBy() {
        return performedBy;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }
}
