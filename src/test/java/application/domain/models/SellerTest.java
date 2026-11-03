package application.domain.models;

import static application.domain.DomainFixtures.address;
import static application.domain.DomainFixtures.admin;
import static application.domain.DomainFixtures.operator;
import static application.domain.DomainFixtures.seller;
import static application.domain.DomainFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import application.domain.enums.UserRole;
import application.domain.exceptions.seller.SelfRegistrationNotAllowedException;
import application.domain.exceptions.user.InvalidRoleException;
import application.domain.valueobjects.TaxId;
import org.junit.jupiter.api.Test;

class SellerTest {

    @Test
    void onlyAdminCanRegisterSellers() {
        User sellerUser = user(UserRole.SELLER);
        assertThatThrownBy(() -> Seller.registerBy(user(UserRole.BUYER), sellerUser, TaxId.of("900123456-7")))
                .isInstanceOf(SelfRegistrationNotAllowedException.class);
        assertThat(seller(admin()).getUser().getRole()).isEqualTo(UserRole.SELLER);
    }

    @Test
    void sellerUserMustHaveSellerRole() {
        assertThatThrownBy(() -> Seller.registerBy(admin(), user(UserRole.BUYER), TaxId.of("900123456-7")))
                .isInstanceOf(InvalidRoleException.class);
    }

    @Test
    void canAssignOwnWarehouseOnlyOnce() {
        Seller seller = seller(admin());
        Warehouse warehouse = Warehouse.createSellerWarehouse("W", address(), seller, operator());
        seller.assignWarehouse(warehouse);
        assertThat(seller.hasWarehouses()).isTrue();
        assertThatThrownBy(() -> seller.assignWarehouse(warehouse)).isInstanceOf(RuntimeException.class);
    }
}
