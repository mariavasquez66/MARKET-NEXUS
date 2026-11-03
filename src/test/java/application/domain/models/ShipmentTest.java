package application.domain.models;

import static application.domain.DomainFixtures.address;
import static application.domain.DomainFixtures.admin;
import static application.domain.DomainFixtures.buyer;
import static application.domain.DomainFixtures.cartWith;
import static application.domain.DomainFixtures.operator;
import static application.domain.DomainFixtures.publishedDigitalProduct;
import static application.domain.DomainFixtures.publishedPhysicalProduct;
import static application.domain.DomainFixtures.seller;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import application.domain.enums.ShipmentStatus;
import application.domain.exceptions.common.InvalidDataException;
import application.domain.exceptions.product.InvalidProductTypeException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ShipmentTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    @Test
    void digitalOrdersDoNotRequireShipment() {
        Product product = publishedDigitalProduct(seller(admin()));
        Order order = Order.fromCart(cartWith(buyer(), product, 1), address(), NOW);
        Warehouse warehouse = Warehouse.createMarketplace("W", address(), operator());
        assertThatThrownBy(() -> Shipment.create(order, warehouse))
                .isInstanceOf(InvalidProductTypeException.class);
    }

    @Test
    void shipmentRequiresTrackingBeforeDelivery() {
        Product product = publishedPhysicalProduct(seller(admin()), operator());
        Order order = Order.fromCart(cartWith(buyer(), product, 1), address(), NOW);
        order.confirmPayment(NOW);
        Shipment shipment = Shipment.create(order, product.getSeller().getWarehouses().get(0));
        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.PENDING);
        assertThatThrownBy(shipment::confirmDelivery)
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> shipment.startShipping(" "))
                .isInstanceOf(InvalidDataException.class);
        shipment.startShipping("TRACK-1");
        shipment.confirmDelivery();
        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.DELIVERED);
    }
}
