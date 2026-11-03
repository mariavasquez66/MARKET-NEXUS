package application.domain.models;

import static application.domain.DomainFixtures.address;
import static application.domain.DomainFixtures.buyer;
import static application.domain.DomainFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import application.domain.enums.CommercialStatus;
import application.domain.enums.UserRole;
import application.domain.exceptions.buyer.RestrictedBuyerException;
import application.domain.exceptions.common.InvalidDataException;
import application.domain.exceptions.user.InvalidRoleException;
import application.domain.valueobjects.Address;
import org.junit.jupiter.api.Test;

class BuyerTest {

    @Test
    void createRequiresBuyerRole() {
        assertThatThrownBy(() -> Buyer.create(user(UserRole.SELLER), address()))
                .isInstanceOf(InvalidRoleException.class);
        assertThat(buyer().getCommercialStatus()).isEqualTo(CommercialStatus.ELIGIBLE);
    }

    @Test
    void restrictedBuyerCannotPurchase() {
        Buyer buyer = buyer();
        buyer.restrict();
        assertThatThrownBy(buyer::assertCanPurchase)
                .isInstanceOf(RestrictedBuyerException.class);
        buyer.markEligible();
        buyer.assertCanPurchase();
    }

    @Test
    void additionalAddressesAreUnique() {
        Buyer buyer = buyer();
        Address second = Address.of("Calle 2", "Medellin", "Antioquia", "050001", "Colombia");
        buyer.addAdditionalAddress(second);
        assertThatThrownBy(() -> buyer.addAdditionalAddress(second))
                .isInstanceOf(InvalidDataException.class);
        assertThat(buyer.getAdditionalAddresses()).hasSize(1);
    }
}
