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

import application.domain.enums.RefundStatus;
import application.domain.exceptions.refund.InvalidRefundAmountException;
import application.domain.exceptions.refund.RefundAlreadyProcessedException;
import application.domain.exceptions.returns.ReturnNotAllowedException;
import application.domain.valueobjects.Money;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class RefundTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    private Return deliveredAndReturnedOrder() {
        Product product = publishedPhysicalProduct(seller(admin()), operator());
        Order order = Order.fromCart(cartWith(buyer(), product, 1), address(), NOW);
        order.confirmPayment(NOW);
        order.markDispatched();
        order.markDelivered();
        return Return.request(order, "Broken", NOW);
    }

    @Test
    void refundRequiresApprovedReturn() {
        Return returnRequest = deliveredAndReturnedOrder();
        assertThatThrownBy(() -> Refund.create(returnRequest, Money.cop(new BigDecimal("10")),
                returnRequest.getOrder().getTotalAmount()))
                .isInstanceOf(ReturnNotAllowedException.class);
    }

    @Test
    void refundCannotExceedOrderTotalOrBeProcessedTwice() {
        Return returnRequest = deliveredAndReturnedOrder();
        returnRequest.approve();
        Money total = returnRequest.getOrder().getTotalAmount();
        assertThatThrownBy(() -> Refund.create(returnRequest,
                total.add(Money.cop(BigDecimal.ONE)), total))
                .isInstanceOf(InvalidRefundAmountException.class);
        Refund refund = Refund.create(returnRequest, Money.cop(new BigDecimal("50.00")), total);
        refund.process();
        assertThat(refund.getStatus()).isEqualTo(RefundStatus.PROCESSED);
        assertThatThrownBy(refund::process).isInstanceOf(RefundAlreadyProcessedException.class);
    }
}
