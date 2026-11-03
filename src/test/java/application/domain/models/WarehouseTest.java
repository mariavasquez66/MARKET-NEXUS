package application.domain.models;

import static application.domain.DomainFixtures.address;
import static application.domain.DomainFixtures.admin;
import static application.domain.DomainFixtures.buyer;
import static application.domain.DomainFixtures.operator;
import static application.domain.DomainFixtures.seller;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import application.domain.enums.WarehouseType;
import application.domain.exceptions.warehouse.InvalidWarehouseTypeException;
import org.junit.jupiter.api.Test;

class WarehouseTest {

    @Test
    void marketplaceWarehouseHasNoOwner() {
        Warehouse warehouse = Warehouse.createMarketplace("Central", address(), operator());
        assertThat(warehouse.getType()).isEqualTo(WarehouseType.MARKETPLACE);
        assertThat(warehouse.getOwner()).isNull();
    }

    @Test
    void sellerWarehouseRequiresSellerOwner() {
        Seller seller = seller(admin());
        Warehouse warehouse = Warehouse.createSellerWarehouse("Seller", address(), seller, operator());
        assertThat(warehouse.getOwner().getId()).isEqualTo(seller.getId());
        assertThatThrownBy(() -> Warehouse.createSellerWarehouse("Seller", address(), null, operator()))
                .isInstanceOf(InvalidWarehouseTypeException.class);
    }

    @Test
    void responsibleMustBeAdminOrOperator() {
        assertThatThrownBy(() -> Warehouse.createMarketplace("Central", address(), buyer().getUser()))
                .isInstanceOf(InvalidWarehouseTypeException.class);
        assertThatThrownBy(() -> Warehouse.createMarketplace("Central", address(), null))
                .isInstanceOf(InvalidWarehouseTypeException.class);
    }
}
