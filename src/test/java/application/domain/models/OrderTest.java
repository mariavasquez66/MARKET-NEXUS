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

import application.domain.enums.OrderStatus;
import application.domain.enums.PaymentStatus;
import application.domain.exceptions.cart.EmptyCartException;
import application.domain.exceptions.order.CancelledOrderException;
import application.domain.exceptions.order.FinalizedOrderException;
import application.domain.exceptions.order.InvalidStateTransitionException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class OrderTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    private Order paidPhysicalOrder() {
        Product product = publishedPhysicalProduct(seller(admin()), operator());
        Order order = Order.fromCart(cartWith(buyer(), product, 1), address(), NOW);
        order.confirmPayment(NOW);
        return order;
    }

    @Test
    void emptyCartCannotBecomeOrder() {
        Product product = publishedPhysicalProduct(seller(admin()), operator());
        Cart cart = Cart.create(buyer());
        assertThatThrownBy(() -> Order.fromCart(cart, address(), NOW))
                .isInstanceOf(EmptyCartException.class);
    }

    @Test
    void physicalOrderFollowsHappyPath() {
        Order order = paidPhysicalOrder();
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.CONFIRMED);
        order.markDispatched();
        order.markDelivered();
        order.finalizeOrder();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.FINALIZED);
    }

    @Test
    void digitalOrderSkipsDispatch() {
        Product product = publishedDigitalProduct(seller(admin()));
        Order order = Order.fromCart(cartWith(buyer(), product, 1), address(), NOW);
        order.confirmPayment(NOW);
        assertThatThrownBy(order::markDispatched).isInstanceOf(InvalidStateTransitionException.class);
        order.markDelivered();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    void onlyPendingPaymentOrdersCanBeCancelled() {
        Order order = paidPhysicalOrder();
        assertThatThrownBy(order::cancel).isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void finalizedAndCancelledOrdersAreNotModifiable() {
        Order finalized = paidPhysicalOrder();
        finalized.markDispatched();
        finalized.markDelivered();
        finalized.finalizeOrder();
        assertThatThrownBy(finalized::assertModifiable).isInstanceOf(FinalizedOrderException.class);

        Product product = publishedDigitalProduct(seller(admin()));
        Order cancelled = Order.fromCart(cartWith(buyer(), product, 1), address(), NOW);
        cancelled.cancel();
        assertThatThrownBy(cancelled::assertModifiable).isInstanceOf(CancelledOrderException.class);
    }

    @Test
    void paymentCanOnlyBeConfirmedOnce() {
        Order order = paidPhysicalOrder();
        assertThatThrownBy(() -> order.confirmPayment(NOW))
                .isInstanceOf(InvalidStateTransitionException.class);
    }
}
