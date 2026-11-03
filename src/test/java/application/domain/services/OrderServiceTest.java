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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import application.domain.enums.ShipmentStatus;
import application.domain.enums.UserRole;
import application.domain.exceptions.common.UnauthorizedOperationException;
import application.domain.exceptions.inventory.InsufficientStockException;
import application.domain.exceptions.order.InvalidStateTransitionException;
import application.domain.models.Buyer;
import application.domain.models.Cart;
import application.domain.models.Inventory;
import application.domain.models.Invoice;
import application.domain.models.Order;
import application.domain.models.Product;
import application.domain.models.Shipment;
import application.domain.models.User;
import application.domain.models.Warehouse;
import application.domain.ports.out.AuditPort;
import application.domain.ports.out.BuyerRepositoryPort;
import application.domain.ports.out.CartRepositoryPort;
import application.domain.ports.out.ClockPort;
import application.domain.ports.out.InventoryMovementRepositoryPort;
import application.domain.ports.out.InventoryRepositoryPort;
import application.domain.ports.out.OrderRepositoryPort;
import application.domain.ports.out.ProductRepositoryPort;
import application.domain.ports.out.ShipmentRepositoryPort;
import application.domain.valueobjects.Money;
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
class OrderServiceTest {
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Mock CartRepositoryPort cartRepository;
    @Mock BuyerRepositoryPort buyerRepository;
    @Mock OrderRepositoryPort orderRepository;
    @Mock ProductRepositoryPort productRepository;
    @Mock InventoryRepositoryPort inventoryRepository;
    @Mock InventoryMovementRepositoryPort movementRepository;
    @Mock ShipmentRepositoryPort shipmentRepository;
    @Mock InvoiceService invoiceService;
    @Mock AuditPort auditPort;
    @Mock ClockPort clockPort;

    private OrderService service;

    @BeforeEach
    void setUp() {
        service = new OrderService(cartRepository, buyerRepository, orderRepository,
                productRepository, inventoryRepository, movementRepository, shipmentRepository,
                invoiceService, auditPort, clockPort);
        when(clockPort.now()).thenReturn(NOW);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void checkoutReservesStockAndClearsCart() {
        Buyer buyer = buyer();
        Product product = publishedPhysicalProduct(seller(admin()), operator());
        Cart cart = cartWith(buyer, product, 2);
        Inventory inventory = Inventory.create(product, product.getSeller().getWarehouses().get(0), 5);
        when(buyerRepository.findByUserId(buyer.getUser().getId())).thenReturn(Optional.of(buyer));
        when(cartRepository.findByBuyerId(buyer.getId())).thenReturn(Optional.of(cart));
        when(inventoryRepository.findByProductId(product.getId())).thenReturn(List.of(inventory));

        Order order = service.checkout(buyer.getUser(), address());

        assertThat(order.getAllocations()).hasSize(1);
        assertThat(order.getAllocations().get(0).getQuantity()).isEqualTo(2);
        assertThat(inventory.getReservedQuantity()).isEqualTo(2);
        assertThat(cart.getItems()).isEmpty();
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void checkoutReleasesReservationsWhenStockIsInsufficient() {
        Buyer buyer = buyer();
        Product product = publishedPhysicalProduct(seller(admin()), operator());
        Cart cart = cartWith(buyer, product, 3);
        Inventory inventory = Inventory.create(product, product.getSeller().getWarehouses().get(0), 1);
        when(buyerRepository.findByUserId(buyer.getUser().getId())).thenReturn(Optional.of(buyer));
        when(cartRepository.findByBuyerId(buyer.getId())).thenReturn(Optional.of(cart));
        when(inventoryRepository.findByProductId(product.getId())).thenReturn(List.of(inventory));

        assertThatThrownBy(() -> service.checkout(buyer.getUser(), address()))
                .isInstanceOf(InsufficientStockException.class);
        assertThat(inventory.getAvailableQuantity()).isEqualTo(1);
        assertThat(inventory.getReservedQuantity()).isZero();
        verify(orderRepository, never()).save(any());
    }

    @Test
    void confirmPaymentConfirmsSaleAndIssuesInvoice() {
        Buyer buyer = buyer();
        Product product = publishedPhysicalProduct(seller(admin()), operator());
        Cart cart = cartWith(buyer, product, 1);
        Order order = Order.fromCart(cart, address(), NOW);
        Warehouse warehouse = product.getSeller().getWarehouses().get(0);
        order.assignAllocations(List.of(StockAllocation.of(product.getId(), warehouse.getId(), 1)));
        Inventory inventory = Inventory.create(product, warehouse, 2);
        inventory.reserve(1);
        Invoice invoice = Invoice.create(paidCopy(order), "INV-1", new BigDecimal("0.19"), NOW);

        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(inventoryRepository.findByProductIdAndWarehouseId(product.getId(), warehouse.getId()))
                .thenReturn(Optional.of(inventory));
        when(invoiceService.generateInvoice(order.getBuyer().getUser(), order.getId())).thenReturn(invoice);

        Order paid = service.confirmPayment(order.getBuyer().getUser(), order.getId());

        assertThat(paid.getStatus()).isEqualTo(application.domain.enums.OrderStatus.PAID);
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
        when(invoiceService.generateInvoice(order.getBuyer().getUser(), order.getId())).thenReturn(invoice);

        Order paid = service.confirmPayment(order.getBuyer().getUser(), order.getId());

        assertThat(paid.getStatus()).isEqualTo(application.domain.enums.OrderStatus.DELIVERED);
    }

    @Test
    void cancelReleasesReservationsOnlyForOwner() {
        Buyer buyer = buyer();
        Product product = publishedPhysicalProduct(seller(admin()), operator());
        Order order = Order.fromCart(cartWith(buyer, product, 2), address(), NOW);
        Warehouse warehouse = product.getSeller().getWarehouses().get(0);
        order.assignAllocations(List.of(StockAllocation.of(product.getId(), warehouse.getId(), 2)));
        Inventory inventory = Inventory.create(product, warehouse, 5);
        inventory.reserve(2);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(inventoryRepository.findByProductIdAndWarehouseId(product.getId(), warehouse.getId()))
                .thenReturn(Optional.of(inventory));

        assertThatThrownBy(() -> service.cancel(buyer().getUser(), order.getId()))
                .isInstanceOf(UnauthorizedOperationException.class);
        service.cancel(buyer.getUser(), order.getId());
        assertThat(inventory.getAvailableQuantity()).isEqualTo(5);
    }

    @Test
    void onlyAuthorizedRolesCanViewOrders() {
        Buyer buyer = buyer();
        Product product = publishedPhysicalProduct(seller(admin()), operator());
        Order order = Order.fromCart(cartWith(buyer, product, 1), address(), NOW);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        assertThat(service.getOrder(buyer.getUser(), order.getId())).isSameAs(order);
        assertThatThrownBy(() -> service.getOrder(buyer().getUser(), order.getId()))
                .isInstanceOf(UnauthorizedOperationException.class);
    }

    @Test
    void dispatchRequiresAllAllocationsInTransit() {
        Buyer buyer = buyer();
        Product product = publishedPhysicalProduct(seller(admin()), operator());
        Order order = Order.fromCart(cartWith(buyer, product, 1), address(), NOW);
        order.confirmPayment(NOW);
        Warehouse warehouse = product.getSeller().getWarehouses().get(0);
        order.assignAllocations(List.of(StockAllocation.of(product.getId(), warehouse.getId(), 1)));
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(shipmentRepository.findByOrderIdAndOriginWarehouseId(order.getId(), warehouse.getId()))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.markDispatched(operator(), order.getId()))
                .isInstanceOf(InvalidStateTransitionException.class);

        Shipment shipment = Shipment.create(order, warehouse);
        shipment.startShipping("TRACK");
        when(shipmentRepository.findByOrderIdAndOriginWarehouseId(order.getId(), warehouse.getId()))
                .thenReturn(List.of(shipment));
        assertThat(service.markDispatched(operator(), order.getId()).getStatus())
                .isEqualTo(application.domain.enums.OrderStatus.DISPATCHED);
    }

    private Order paidCopy(Order order) {
        Order copy = Order.fromCart(Cart.reconstitute("copy", order.getBuyer(), order.getItems()),
                order.getDeliveryAddress(), NOW);
        copy.confirmPayment(NOW);
        return copy;
    }
}
