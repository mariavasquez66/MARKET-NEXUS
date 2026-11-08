package application.domain.services;

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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import application.domain.enums.OrderStatus;
import application.domain.enums.PaymentStatus;
import application.domain.exceptions.common.UnauthorizedOperationException;
import application.domain.models.Buyer;
import application.domain.models.Cart;
import application.domain.models.Inventory;
import application.domain.models.Invoice;
import application.domain.models.Order;
import application.domain.models.Product;
import application.domain.models.Warehouse;
import application.domain.ports.out.AuditPort;
import application.domain.ports.out.ClockPort;
import application.domain.ports.out.InventoryMovementRepositoryPort;
import application.domain.ports.out.InventoryRepositoryPort;
import application.domain.ports.out.OrderRepositoryPort;
import application.domain.valueobjects.StockAllocation;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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
class OrderPaymentServiceTest {
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Mock OrderRepositoryPort orderRepository;
    @Mock InventoryRepositoryPort inventoryRepository;
    @Mock InventoryMovementRepositoryPort movementRepository;
    @Mock InvoiceService invoiceService;
    @Mock AuditPort auditPort;
    @Mock ClockPort clockPort;

    private OrderPaymentService service;

    @BeforeEach
    void setUp() {
        service = new OrderPaymentService(orderRepository, inventoryRepository, movementRepository,
                invoiceService, auditPort, clockPort);
        when(clockPort.now()).thenReturn(NOW);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryRepository.save(any(Inventory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void confirmPaymentConfirmsSaleAndIssuesInvoice() {
        Buyer buyer = buyer();
        Product product = publishedPhysicalProduct(seller(admin()), operator());
        Warehouse warehouse = product.getSeller().getWarehouses().get(0);
        Order order = Order.fromCart(cartWith(buyer, product, 1), address(), NOW);
        order.assignAllocations(List.of(StockAllocation.of(product.getId(), warehouse.getId(), 1)));
        Inventory inventory = Inventory.create(product, warehouse, 2);
        inventory.reserve(1);
        Invoice invoice = Invoice.create(paidCopy(order), "INV-1", new BigDecimal("0.19"), NOW);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(inventoryRepository.findByProductIdAndWarehouseId(product.getId(), warehouse.getId()))
                .thenReturn(Optional.of(inventory));
        when(invoiceService.generateInvoice(buyer.getUser(), order.getId())).thenReturn(invoice);

        Order paid = service.confirmPayment(buyer.getUser(), order.getId());

        assertThat(paid.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(paid.getPaymentStatus()).isEqualTo(PaymentStatus.CONFIRMED);
        assertThat(paid.getPaidAt()).isEqualTo(NOW);
        assertThat(paid.getInvoice()).isSameAs(invoice);
        assertThat(inventory.getReservedQuantity()).isZero();
        verify(movementRepository).save(any());
    }

    @Test
    void digitalOrderIsDeliveredImmediatelyAfterPayment() {
        Buyer buyer = buyer();
        Product product = publishedDigitalProduct(seller(admin()));
        Order order = Order.fromCart(cartWith(buyer, product, 1), address(), NOW);
        Invoice invoice = Invoice.create(paidCopy(order), "INV-1", new BigDecimal("0.19"), NOW);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(invoiceService.generateInvoice(buyer.getUser(), order.getId())).thenReturn(invoice);

        Order paid = service.confirmPayment(buyer.getUser(), order.getId());

        assertThat(paid.getStatus()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    void paymentRejectsBuyerWhoDoesNotOwnTheOrder() {
        Buyer owner = buyer();
        Product product = publishedDigitalProduct(seller(admin()));
        Order order = Order.fromCart(cartWith(owner, product, 1), address(), NOW);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.confirmPayment(buyer().getUser(), order.getId()))
                .isInstanceOf(UnauthorizedOperationException.class);
    }

    private Order paidCopy(Order order) {
        Order copy = Order.fromCart(Cart.reconstitute("copy", order.getBuyer(), order.getItems()),
                order.getDeliveryAddress(), NOW);
        copy.confirmPayment(NOW);
        return copy;
    }
}
