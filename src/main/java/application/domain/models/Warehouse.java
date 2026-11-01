package application.domain.models;

import application.domain.enums.UserRole;
import application.domain.enums.WarehouseType;
import application.domain.exceptions.common.InvalidDataException;
import application.domain.exceptions.warehouse.InvalidWarehouseTypeException;
import application.domain.valueobjects.Address;
import java.util.Objects;
import java.util.UUID;

public class Warehouse {
    private final String id;
    private String name;
    private Address location;
    private final WarehouseType type;
    private final Seller owner;
    private User responsible;

    private Warehouse(String id,
                      String name,
                      Address location,
                      WarehouseType type,
                      Seller owner,
                      User responsible) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.name = requireText(name, "Warehouse name is required");
        this.location = Objects.requireNonNull(location, "location cannot be null");
        this.type = Objects.requireNonNull(type, "type cannot be null");
        this.owner = owner;
        this.responsible = Objects.requireNonNull(responsible, "responsible cannot be null");
    }

    public static Warehouse createMarketplace(String name, Address location, User responsible) {
        assertResponsible(responsible);
        return new Warehouse(UUID.randomUUID().toString(), name, location,
                WarehouseType.MARKETPLACE, null, responsible);
    }

    public static Warehouse createSellerWarehouse(String name,
                                                  Address location,
                                                  Seller owner,
                                                  User responsible) {
        if (owner == null) {
            throw new InvalidWarehouseTypeException("Seller warehouse requires an owner");
        }
        assertResponsible(responsible);
        return new Warehouse(UUID.randomUUID().toString(), name, location,
                WarehouseType.SELLER, owner, responsible);
    }

    public static Warehouse reconstitute(String id,
                                         String name,
                                         Address location,
                                         WarehouseType type,
                                         Seller owner,
                                         User responsible) {
        if (type.equals(WarehouseType.SELLER) && owner == null) {
            throw new InvalidWarehouseTypeException("Seller warehouse requires an owner");
        }
        if (type.equals(WarehouseType.MARKETPLACE) && owner != null) {
            throw new InvalidWarehouseTypeException("Marketplace warehouse cannot have an owner");
        }
        return new Warehouse(id, name, location, type, owner, responsible);
    }

    public void changeName(String name) {
        this.name = requireText(name, "Warehouse name is required");
    }

    public void changeLocation(Address location) {
        this.location = Objects.requireNonNull(location, "location cannot be null");
    }

    public void assignResponsible(User responsible) {
        assertResponsible(responsible);
        this.responsible = responsible;
    }

    public void assertOperational() {
        if (responsible == null) {
            throw new InvalidDataException("Warehouse has no responsible user");
        }
    }

    private static void assertResponsible(User responsible) {
        if (responsible == null) {
            throw new InvalidWarehouseTypeException("Warehouse responsible is required");
        }
        if (!responsible.hasRole(UserRole.ADMIN) && !responsible.hasRole(UserRole.LOGISTIC_OPERATOR)) {
            throw new InvalidWarehouseTypeException("Warehouse responsible must be ADMIN or LOGISTIC_OPERATOR");
        }
        responsible.assertCanOperate();
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new InvalidDataException(message);
        }
        return value.trim();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Address getLocation() {
        return location;
    }

    public WarehouseType getType() {
        return type;
    }

    public Seller getOwner() {
        return owner;
    }

    public User getResponsible() {
        return responsible;
    }
}
