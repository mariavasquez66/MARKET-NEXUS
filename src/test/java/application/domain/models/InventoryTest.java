package application.domain.models;

import static application.domain.DomainFixtures.admin;
import static application.domain.DomainFixtures.address;
import static application.domain.DomainFixtures.operator;
import static application.domain.DomainFixtures.publishedDigitalProduct;
import static application.domain.DomainFixtures.publishedPhysicalProduct;
import static application.domain.DomainFixtures.seller;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import application.domain.enums.InventoryCondition;
import application.domain.exceptions.inventory.DamagedInventoryException;
import application.domain.exceptions.inventory.InsufficientStockException;
import application.domain.exceptions.inventory.InvalidReservationException;
import application.domain.exceptions.inventory.NegativeStockException;
import application.domain.exceptions.product.InvalidProductTypeException;
import org.junit.jupiter.api.Test;

class InventoryTest {

    @Test
    void digitalProductCannotHaveInventory() {
        Product product = publishedDigitalProduct(seller(admin()));
        Warehouse warehouse = Warehouse.createMarketplace("W", address(), operator());
        assertThatThrownBy(() -> Inventory.create(product, warehouse, 1))
                .isInstanceOf(InvalidProductTypeException.class);
    }

    @Test
    void reserveAndConfirmMoveQuantitiesWithoutGoingNegative() {
        Product product = publishedPhysicalProduct(seller(admin()), operator());
        Inventory inventory = Inventory.create(product, product.getSeller().getWarehouses().get(0), 10);
        inventory.reserve(4);
        assertThat(inventory.getAvailableQuantity()).isEqualTo(6);
        assertThat(inventory.getReservedQuantity()).isEqualTo(4);
        inventory.confirmSale(3);
        assertThat(inventory.getReservedQuantity()).isEqualTo(1);
        inventory.releaseReservation(1);
        assertThat(inventory.getAvailableQuantity()).isEqualTo(7);
        assertThatThrownBy(() -> inventory.reserve(8)).isInstanceOf(InsufficientStockException.class);
    }

    @Test
    void adjustmentCannotCreateNegativeStock() {
        Product product = publishedPhysicalProduct(seller(admin()), operator());
        Inventory inventory = Inventory.create(product, product.getSeller().getWarehouses().get(0), 2);
        assertThatThrownBy(() -> inventory.adjust(-3)).isInstanceOf(NegativeStockException.class);
    }

    @Test
    void damagedInventoryCannotBeReservedAndDamagingRequiresNoReservations() {
        Product product = publishedPhysicalProduct(seller(admin()), operator());
        Inventory inventory = Inventory.create(product, product.getSeller().getWarehouses().get(0), 5);
        inventory.reserve(1);
        assertThatThrownBy(inventory::markDamaged).isInstanceOf(InvalidReservationException.class);
        inventory.releaseReservation(1);
        inventory.markDamaged();
        assertThat(inventory.getCondition()).isEqualTo(InventoryCondition.DAMAGED);
        assertThatThrownBy(() -> inventory.reserve(1)).isInstanceOf(DamagedInventoryException.class);
    }

    @Test
    void damagedInventoryCannotBeSold() {
        Product product = publishedPhysicalProduct(seller(admin()), operator());
        Inventory inventory = Inventory.create(product, product.getSeller().getWarehouses().get(0), 5);
        inventory.markDamaged();
        assertThatThrownBy(() -> inventory.confirmSale(1))
                .isInstanceOf(DamagedInventoryException.class);
    }

    @Test
    void reconstituteRejectsNegativeQuantities() {
        Product product = publishedPhysicalProduct(seller(admin()), operator());
        Warehouse warehouse = product.getSeller().getWarehouses().get(0);
        assertThatThrownBy(() -> Inventory.reconstitute(
                "inventory", product, warehouse, -1, 0, InventoryCondition.GOOD))
                .isInstanceOf(NegativeStockException.class);
        assertThatThrownBy(() -> Inventory.reconstitute(
                "inventory", product, warehouse, 0, -1, InventoryCondition.GOOD))
                .isInstanceOf(NegativeStockException.class);
    }
}
