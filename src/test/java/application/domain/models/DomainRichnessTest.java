package application.domain.models;

import static application.domain.DomainFixtures.address;
import static application.domain.DomainFixtures.admin;
import static application.domain.DomainFixtures.buyer;
import static application.domain.DomainFixtures.cartWith;
import static application.domain.DomainFixtures.operator;
import static application.domain.DomainFixtures.publishedPhysicalProduct;
import static application.domain.DomainFixtures.seller;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class DomainRichnessTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
    private static final List<Class<?>> MODELS = List.of(
            AuditLog.class,
            AuthResult.class,
            Buyer.class,
            Cart.class,
            CartItem.class,
            Inventory.class,
            InventoryMovement.class,
            Invoice.class,
            Order.class,
            Product.class,
            Refund.class,
            Return.class,
            Seller.class,
            Shipment.class,
            User.class,
            Warehouse.class);

    @Test
    void modelsDoNotExposePublicSetters() {
        List<String> setters = MODELS.stream()
                .flatMap(type -> Arrays.stream(type.getMethods()))
                .filter(method -> method.getParameterCount() == 1)
                .filter(method -> method.getName().startsWith("set"))
                .map(method -> method.getDeclaringClass().getSimpleName() + "." + method.getName())
                .toList();

        assertThat(setters).isEmpty();
    }

    @Test
    void collectionsAreExposedAsUnmodifiableViews() {
        Buyer buyer = buyer();
        assertThatThrownBy(() -> buyer.getAdditionalAddresses().add(address()))
                .isInstanceOf(UnsupportedOperationException.class);

        Seller seller = seller(admin());
        assertThatThrownBy(() -> seller.getWarehouses().add(
                Warehouse.createMarketplace("W", address(), operator())))
                .isInstanceOf(UnsupportedOperationException.class);

        Product product = publishedPhysicalProduct(seller, operator());
        assertThatThrownBy(() -> product.getVariants().clear())
                .isInstanceOf(UnsupportedOperationException.class);

        Cart cart = cartWith(buyer, product, 1);
        assertThatThrownBy(() -> cart.getItems().clear())
                .isInstanceOf(UnsupportedOperationException.class);

        Order order = Order.fromCart(cart, address(), NOW);
        assertThatThrownBy(() -> order.getItems().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> order.getAllocations().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
