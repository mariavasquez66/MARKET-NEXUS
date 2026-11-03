package application.domain.models;

import static application.domain.DomainFixtures.admin;
import static application.domain.DomainFixtures.buyer;
import static application.domain.DomainFixtures.operator;
import static application.domain.DomainFixtures.publishedPhysicalProduct;
import static application.domain.DomainFixtures.seller;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import application.domain.exceptions.cart.EmptyCartException;
import application.domain.exceptions.cart.InvalidQuantityException;
import application.domain.exceptions.cart.ProductNotAvailableException;
import org.junit.jupiter.api.Test;

class CartTest {

    @Test
    void addingSameProductMergesQuantities() {
        Product product = publishedPhysicalProduct(seller(admin()), operator());
        Cart cart = Cart.create(buyer());
        cart.addItem(product, 2);
        cart.addItem(product, 3);
        assertThat(cart.getItems()).hasSize(1);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(5);
        assertThat(cart.total().getAmount()).isEqualByComparingTo("595.00");
    }

    @Test
    void unpublishedProductCannotBeAdded() {
        Product product = publishedPhysicalProduct(seller(admin()), operator());
        product.suspend();
        Cart cart = Cart.create(buyer());
        assertThatThrownBy(() -> cart.addItem(product, 1))
                .isInstanceOf(ProductNotAvailableException.class);
    }

    @Test
    void quantitiesAndRemovalAreValidated() {
        Product product = publishedPhysicalProduct(seller(admin()), operator());
        Cart cart = Cart.create(buyer());
        assertThatThrownBy(() -> cart.addItem(product, 0)).isInstanceOf(InvalidQuantityException.class);
        cart.addItem(product, 1);
        cart.updateQuantity(product.getId(), 4);
        assertThat(cart.getItems().get(0).getQuantity()).isEqualTo(4);
        cart.removeItem(product.getId());
        assertThatThrownBy(cart::assertNotEmpty).isInstanceOf(EmptyCartException.class);
    }
}
