package application.domain.models;

import static application.domain.DomainFixtures.address;
import static application.domain.DomainFixtures.admin;
import static application.domain.DomainFixtures.buyer;
import static application.domain.DomainFixtures.operator;
import static application.domain.DomainFixtures.publishedPhysicalProduct;
import static application.domain.DomainFixtures.seller;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import application.domain.enums.InvoiceStatus;
import application.domain.exceptions.invoice.VoidedInvoiceException;
import application.domain.exceptions.order.InvalidStateTransitionException;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class InvoiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    @Test
    void invoiceRequiresPaidOrderAndCalculatesTaxes() {
        Product product = publishedPhysicalProduct(seller(admin()), operator());
        Cart cart = Cart.create(buyer());
        cart.addItem(product, 1);
        Order order = Order.fromCart(cart, address(), NOW);
        assertThatThrownBy(() -> Invoice.create(order, "INV-1", new BigDecimal("0.19"), NOW))
                .isInstanceOf(InvalidStateTransitionException.class);
        order.confirmPayment(NOW);
        Invoice invoice = Invoice.create(order, "INV-1", new BigDecimal("0.19"), NOW);
        assertThat(invoice.getTotal().getAmount()).isEqualByComparingTo("119.00");
        assertThat(invoice.getSubtotal().getAmount()).isEqualByComparingTo("100.00");
        assertThat(invoice.getTaxes().getAmount()).isEqualByComparingTo("19.00");
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.ISSUED);
    }

    @Test
    void invoiceCannotBeVoidedTwice() {
        Product product = publishedPhysicalProduct(seller(admin()), operator());
        Cart cart = Cart.create(buyer());
        cart.addItem(product, 1);
        Order order = Order.fromCart(cart, address(), NOW);
        order.confirmPayment(NOW);
        Invoice invoice = Invoice.create(order, "INV-1", new BigDecimal("0.19"), NOW);
        invoice.voidInvoice();
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.VOIDED);
        assertThatThrownBy(invoice::voidInvoice).isInstanceOf(VoidedInvoiceException.class);
    }
}
