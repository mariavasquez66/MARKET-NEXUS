package application.domain.models;

import application.domain.enums.UserRole;
import application.domain.exceptions.seller.SelfRegistrationNotAllowedException;
import application.domain.exceptions.user.InvalidRoleException;
import application.domain.exceptions.warehouse.InvalidWarehouseTypeException;
import application.domain.exceptions.warehouse.WarehouseAlreadyAssignedException;
import application.domain.valueobjects.TaxId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Seller {
    private final String id;
    private final User user;
    private TaxId taxId;
    private final List<Warehouse> warehouses;

    private Seller(String id, User user, TaxId taxId, List<Warehouse> warehouses) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.user = Objects.requireNonNull(user, "user cannot be null");
        this.taxId = Objects.requireNonNull(taxId, "taxId cannot be null");
        this.warehouses = new ArrayList<>(Objects.requireNonNull(warehouses));
    }

    public static Seller registerBy(User adminActor, User sellerUser, TaxId taxId) {
        Objects.requireNonNull(adminActor, "adminActor cannot be null");
        if (!adminActor.hasRole(UserRole.ADMIN)) {
            throw new SelfRegistrationNotAllowedException("Only ADMIN can register sellers");
        }
        adminActor.assertCanOperate();
        if (!sellerUser.hasRole(UserRole.SELLER)) {
            throw new InvalidRoleException("Seller user must have SELLER role");
        }
        return new Seller(UUID.randomUUID().toString(), sellerUser, taxId, List.of());
    }

    public static Seller reconstitute(String id, User user, TaxId taxId, List<Warehouse> warehouses) {
        return new Seller(id, user, taxId, warehouses);
    }

    public void assignWarehouse(Warehouse warehouse) {
        Objects.requireNonNull(warehouse, "warehouse cannot be null");
        if (warehouse.getOwner() == null || !warehouse.getOwner().getId().equals(id)) {
            throw new InvalidWarehouseTypeException("Warehouse must belong to this seller");
        }
        if (warehouses.stream().anyMatch(existing -> existing.getId().equals(warehouse.getId()))) {
            throw new WarehouseAlreadyAssignedException("Warehouse is already assigned to this seller");
        }
        warehouses.add(warehouse);
    }

    public boolean hasWarehouses() {
        return !warehouses.isEmpty();
    }

    public void updateTaxId(TaxId taxId) {
        this.taxId = Objects.requireNonNull(taxId, "taxId cannot be null");
    }

    public String getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public TaxId getTaxId() {
        return taxId;
    }

    public List<Warehouse> getWarehouses() {
        return Collections.unmodifiableList(warehouses);
    }
}
