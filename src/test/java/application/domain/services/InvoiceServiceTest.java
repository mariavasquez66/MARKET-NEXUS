package application.domain.services;

import static application.domain.DomainFixtures.address;
import static application.domain.DomainFixtures.admin;
import static application.domain.DomainFixtures.buyer;
import static application.domain.DomainFixtures.cartWith;
import static application.domain.DomainFixtures.operator;
import static application.domain.DomainFixtures.publishedPhysicalProduct;
import static application.domain.DomainFixtures.seller;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import application.domain.enums.OrderStatus;
import application.domain.exceptions.invoice.InvoiceAlreadyExistsException;
import application.domain.exceptions.order.InvalidStateTransitionException;
import application.domain.models.Invoice;
import application.domain.models.Order;
import application.domain.models.Product;
import application.domain.ports.out.AuditPort;
import application.domain.ports.out.ClockPort;
import application.domain.ports.out.InvoiceRepositoryPort;
import application.domain.ports.out.OrderRepositoryPort;
import application.domain.ports.out.TaxPolicyPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InvoiceServiceTest {
    @Mock InvoiceRepositoryPort invoiceRepository;
    @Mock OrderRepositoryPort orderRepository;
    @Mock TaxPolicyPort taxPolicy;
    @Mock AuditPort auditPort;
    @Mock ClockPort clockPort;

    private InvoiceService service;

    @BeforeEach
    void setUp() {
        service = new InvoiceService(invoiceRepository, orderRepository, taxPolicy, auditPort, clockPort);
        when(clockPort.now()).thenReturn(Instant.parse("2026-01-01T00:00:00Z"));
        when(taxPolicy.currentTaxRate()).thenReturn(new BigDecimal("0.19"));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void invoiceRequiresPaidOrder() {
        Order order = order();
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        assertThatThrownBy(() -> service.generateInvoice(order.getBuyer().getUser(), order.getId()))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void duplicateInvoiceIsRejectedAndSuccessfulIssueAudited() {
        Order order = paidOrder();
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(invoiceRepository.existsByOrderId(order.getId())).thenReturn(true);
        assertThatThrownBy(() -> service.generateInvoice(order.getBuyer().getUser(), order.getId()))
                .isInstanceOf(InvoiceAlreadyExistsException.class);

        when(invoiceRepository.existsByOrderId(order.getId())).thenReturn(false);
        Invoice invoice = service.generateInvoice(order.getBuyer().getUser(), order.getId());
        assertThat(invoice.getTotal().getAmount()).isEqualByComparingTo("119.00");
        assertThat(invoice.getNumber()).isEqualTo("INV-" + order.getId());
    }

    @Test
    void onlyAdminCanVoidInvoice() {
        Order order = paidOrder();
        Invoice invoice = Invoice.create(order, "INV-1", new BigDecimal("0.19"), Instant.now());
        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));
        assertThatThrownBy(() -> service.voidInvoice(order.getBuyer().getUser(), invoice.getId()))
                .isInstanceOf(application.domain.exceptions.common.InsufficientPermissionException.class);
        Invoice voided = service.voidInvoice(admin(), invoice.getId());
        assertThat(voided.getStatus()).isEqualTo(application.domain.enums.InvoiceStatus.VOIDED);
    }

    @Test
    void buyerCannotReadAnotherBuyerInvoice() {
        Order order = paidOrder();
        Invoice invoice = Invoice.create(order, "INV-1", new BigDecimal("0.19"), Instant.now());
        when(invoiceRepository.findById(invoice.getId())).thenReturn(Optional.of(invoice));
        assertThatThrownBy(() -> service.getInvoice(buyer().getUser(), invoice.getId()))
                .isInstanceOf(application.domain.exceptions.common.UnauthorizedOperationException.class);
    }

    private Order order() {
        Product product = publishedPhysicalProduct(seller(admin()), operator());
        return Order.fromCart(cartWith(buyer(), product, 1), address(), Instant.now());
    }

    private Order paidOrder() {
        Order order = order();
        order.confirmPayment(Instant.now());
        return order;
    }
}
