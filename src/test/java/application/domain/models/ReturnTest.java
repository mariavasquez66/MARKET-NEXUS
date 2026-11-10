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

import application.domain.enums.ReturnStatus;
import application.domain.exceptions.common.InvalidDataException;
import application.domain.exceptions.returns.ReturnAlreadyProcessedException;
import application.domain.exceptions.returns.ReturnNotAllowedException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ReturnTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    private Order deliveredOrder() {
        Product product = publishedPhysicalProduct(seller(admin()), operator());
        Order order = Order.fromCart(cartWith(buyer(), product, 1), address(), NOW);
        order.confirmPayment(NOW);
        order.markDispatched();
        order.markDelivered();
        return order;
    }

    @Test
    void returnRequiresDeliveredOrder() {
        Product product = publishedPhysicalProduct(seller(admin()), operator());
        Order pending = Order.fromCart(cartWith(buyer(), product, 1), address(), NOW);
        assertThatThrownBy(() -> Return.request(pending, "Broken", NOW))
                .isInstanceOf(ReturnNotAllowedException.class);
        assertThat(Return.request(deliveredOrder(), "Broken", NOW).getStatus())
                .isEqualTo(ReturnStatus.REQUESTED);
    }

    @Test
    void partialReturnRequiresPurchasedProductAndValidQuantity() {
        Order order = deliveredOrder();
        String productId = order.getItems().get(0).getProduct().getId();

        Return partial = Return.request(order, "Broken", NOW, productId, 1);

        assertThat(partial.getProductId()).isEqualTo(productId);
        assertThat(partial.getQuantity()).isEqualTo(1);
        assertThatThrownBy(() -> Return.request(order, "Broken", NOW, "unknown", 1))
                .isInstanceOf(InvalidDataException.class)
                .hasMessageContaining("not part of the order");
        assertThatThrownBy(() -> Return.request(order, "Broken", NOW, productId, 2))
                .isInstanceOf(InvalidDataException.class)
                .hasMessageContaining("exceeds purchased quantity");
        assertThatThrownBy(() -> Return.request(order, "Broken", NOW, null, 1))
                .isInstanceOf(InvalidDataException.class)
                .hasMessageContaining("requires a product");
    }

    @Test
    void returnCanBeApprovedAndCompletedOnce() {
        Return returnRequest = Return.request(deliveredOrder(), "Broken", NOW);
        returnRequest.approve();
        assertThatThrownBy(returnRequest::reject)
                .isInstanceOf(ReturnAlreadyProcessedException.class);
        returnRequest.complete();
        assertThat(returnRequest.getStatus()).isEqualTo(ReturnStatus.COMPLETED);
        assertThatThrownBy(returnRequest::complete)
                .isInstanceOf(ReturnAlreadyProcessedException.class);
    }
}
