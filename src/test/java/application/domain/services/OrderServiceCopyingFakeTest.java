package application.domain.services;

import static application.domain.DomainFixtures.address;
import static application.domain.DomainFixtures.admin;
import static application.domain.DomainFixtures.buyer;
import static application.domain.DomainFixtures.cartWith;
import static application.domain.DomainFixtures.operator;
import static application.domain.DomainFixtures.seller;
import static application.domain.DomainFixtures.sellerWarehouse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import application.domain.enums.AuditAction;
import application.domain.enums.OrderStatus;
import application.domain.enums.PaymentStatus;
import application.domain.enums.ProductType;
import application.domain.enums.ShipmentStatus;
import application.domain.exceptions.order.InvalidStateTransitionException;
import application.domain.models.AuditLog;
import application.domain.models.Buyer;
import application.domain.models.Cart;
import application.domain.models.Inventory;
import application.domain.models.InventoryMovement;
import application.domain.models.Invoice;
import application.domain.models.Order;
import application.domain.models.Product;
import application.domain.models.Seller;
import application.domain.models.Shipment;
import application.domain.models.User;
import application.domain.models.Warehouse;
import application.domain.ports.out.AuditPort;
import application.domain.ports.out.BuyerRepositoryPort;
import application.domain.ports.out.CartRepositoryPort;
import application.domain.ports.out.ClockPort;
import application.domain.ports.out.InventoryMovementRepositoryPort;
import application.domain.ports.out.InventoryRepositoryPort;
import application.domain.ports.out.InvoiceRepositoryPort;
import application.domain.ports.out.OrderRepositoryPort;
import application.domain.ports.out.ProductRepositoryPort;
import application.domain.ports.out.ShipmentRepositoryPort;
import application.domain.ports.out.WarehouseRepositoryPort;
import application.domain.valueobjects.Money;
import application.domain.valueobjects.StockAllocation;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceCopyingFakeTest {
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Mock BuyerRepositoryPort buyerRepository;
    @Mock CartRepositoryPort cartRepository;
    @Mock ProductRepositoryPort productRepository;
    @Mock WarehouseRepositoryPort warehouseRepository;

    private InvoiceService invoiceService;
    private CopyingInvoiceRepository invoiceRepository;
    private CopyingOrderRepository orderRepository;
    private CopyingInventoryRepository inventoryRepository;
    private CopyingShipmentRepository shipmentRepository;
    private RecordingMovementRepository movementRepository;
    private FixedClock clock;
    private OrderService service;

    @BeforeEach
    void setUp() {
        orderRepository = new CopyingOrderRepository();
        inventoryRepository = new CopyingInventoryRepository();
        shipmentRepository = new CopyingShipmentRepository();
        movementRepository = new RecordingMovementRepository();
        clock = new FixedClock(NOW);
        invoiceRepository = new CopyingInvoiceRepository();
        invoiceService = new InvoiceService(invoiceRepository, orderRepository,
                () -> new BigDecimal("0.19"), new NoopAuditPort(), clock);
        service = new OrderService(cartRepository, buyerRepository, orderRepository,
                productRepository, inventoryRepository, movementRepository, shipmentRepository,
                invoiceService, new NoopAuditPort(), clock);
    }

    @Test
    void confirmPaymentPersistsPaidStateWithCopyingRepositories() {
        Buyer buyer = buyer();
        Product product = publishedPhysicalProduct();
        Warehouse warehouse = product.getSeller().getWarehouses().get(0);
        Order order = paidReadyOrder(buyer, product, warehouse);
        Inventory inventory = Inventory.create(product, warehouse, 2);
        inventory.reserve(1);
        orderRepository.seed(order);
        inventoryRepository.seed(inventory);

        Order result = service.confirmPayment(buyer.getUser(), order.getId());

        Order persisted = orderRepository.stored(order.getId());
        Inventory persistedInventory = inventoryRepository.stored(inventory.getId());
        Invoice invoice = invoiceRepository.findByOrderId(order.getId()).orElseThrow();
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(persisted.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(persisted.getPaymentStatus()).isEqualTo(PaymentStatus.CONFIRMED);
        assertThat(persisted.getInvoice().getId()).isEqualTo(invoice.getId());
        assertThat(invoice.getOrder().getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(persistedInventory.getReservedQuantity()).isZero();
        assertThat(movementRepository.saved).hasSize(1);
        assertThat(movementRepository.saved.get(0).getType().getCode()).isEqualTo("SALE");
    }

    @Test
    void dispatchAcrossTwoWarehousesRequiresEveryShipmentInTransitWithCopyingRepositories() {
        Buyer buyer = buyer();
        Seller seller = seller(admin());
        Warehouse first = sellerWarehouse(seller, operator());
        Warehouse second = Warehouse.createSellerWarehouse(
                "Second warehouse", address(), seller, operator());
        seller.assignWarehouse(second);
        Product product = Product.create(seller, "Laptop", "Laptop description",
                ProductType.PHYSICAL, Money.cop(new BigDecimal("119.00")));
        product.publish();
        Order order = Order.fromCart(cartWith(buyer, product, 2), address(), NOW);
        order.confirmPayment(NOW);
        order.assignAllocations(List.of(
                StockAllocation.of(product.getId(), first.getId(), 1),
                StockAllocation.of(product.getId(), second.getId(), 1)));
        orderRepository.seed(order);

        Shipment inTransitFirst = Shipment.create(order, first);
        inTransitFirst.startShipping("TRACK-1");
        Shipment pendingFirst = Shipment.create(order, first);
        Shipment inTransitSecond = Shipment.create(order, second);
        inTransitSecond.startShipping("TRACK-2");
        shipmentRepository.seed(inTransitFirst);
        shipmentRepository.seed(pendingFirst);
        shipmentRepository.seed(inTransitSecond);

        assertThatThrownBy(() -> service.markDispatched(operator(), order.getId()))
                .isInstanceOf(InvalidStateTransitionException.class);

        pendingFirst.startShipping("TRACK-3");
        shipmentRepository.seed(pendingFirst);

        Order dispatched = service.markDispatched(operator(), order.getId());

        assertThat(dispatched.getStatus()).isEqualTo(OrderStatus.DISPATCHED);
        assertThat(orderRepository.stored(order.getId()).getStatus())
                .isEqualTo(OrderStatus.DISPATCHED);
        assertThat(shipmentRepository.findByOrderIdAndOriginWarehouseId(order.getId(), first.getId()))
                .extracting(Shipment::getStatus)
                .containsExactlyInAnyOrder(ShipmentStatus.IN_TRANSIT, ShipmentStatus.IN_TRANSIT);
        assertThat(shipmentRepository.findByOrderIdAndOriginWarehouseId(order.getId(), second.getId()))
                .extracting(Shipment::getStatus)
                .containsExactly(ShipmentStatus.IN_TRANSIT);
    }

    @Test
    void startShippingDispatchesOnlyAfterEveryWarehouseShipmentIsInTransit() {
        Buyer buyer = buyer();
        Seller seller = seller(admin());
        Warehouse first = sellerWarehouse(seller, operator());
        Warehouse second = Warehouse.createSellerWarehouse(
                "Second warehouse", address(), seller, operator());
        seller.assignWarehouse(second);
        Product product = Product.create(seller, "Laptop", "Laptop description",
                ProductType.PHYSICAL, Money.cop(new BigDecimal("119.00")));
        product.publish();
        Order order = Order.fromCart(cartWith(buyer, product, 2), address(), NOW);
        order.confirmPayment(NOW);
        order.assignAllocations(List.of(
                StockAllocation.of(product.getId(), first.getId(), 1),
                StockAllocation.of(product.getId(), second.getId(), 1)));
        orderRepository.seed(order);
        Shipment firstShipment = Shipment.create(order, first);
        Shipment secondShipment = Shipment.create(order, second);
        shipmentRepository.seed(firstShipment);
        shipmentRepository.seed(secondShipment);
        LogisticsService logistics = new LogisticsService(shipmentRepository, orderRepository,
                warehouseRepository, service, new NoopAuditPort(), clock);

        logistics.startShipping(operator(), firstShipment.getId(), "TRACK-1");

        assertThat(orderRepository.stored(order.getId()).getStatus()).isEqualTo(OrderStatus.PAID);

        logistics.startShipping(operator(), secondShipment.getId(), "TRACK-2");

        assertThat(orderRepository.stored(order.getId()).getStatus())
                .isEqualTo(OrderStatus.DISPATCHED);
    }

    private static Product publishedPhysicalProduct() {
        Seller seller = seller(admin());
        sellerWarehouse(seller, operator());
        Product product = Product.create(seller, "Laptop", "Laptop description",
                ProductType.PHYSICAL, Money.cop(new BigDecimal("119.00")));
        product.publish();
        return product;
    }

    private static Order paidReadyOrder(Buyer buyer, Product product, Warehouse warehouse) {
        Order order = Order.fromCart(cartWith(buyer, product, 1), address(), NOW);
        order.assignAllocations(List.of(
                StockAllocation.of(product.getId(), warehouse.getId(), 1)));
        return order;
    }

    private static Order copyOf(Order order) {
        return Order.reconstitute(order.getId(), order.getBuyer(), order.getItems(),
                order.getStatus(), order.getTotalAmount(), order.getPaymentStatus(),
                order.getAllocations(), order.getDeliveryAddress(), order.getInvoice(),
                order.getCreatedAt(), order.getPaidAt());
    }

    private static Inventory copyOf(Inventory inventory) {
        return Inventory.reconstitute(inventory.getId(), inventory.getProduct(),
                inventory.getWarehouse(), inventory.getAvailableQuantity(),
                inventory.getReservedQuantity(), inventory.getCondition());
    }

    private static Invoice copyOf(Invoice invoice) {
        return Invoice.reconstitute(invoice.getId(), copyOf(invoice.getOrder()), invoice.getNumber(),
                invoice.getSubtotal(), invoice.getTaxes(), invoice.getTotal(), invoice.getStatus(),
                invoice.getIssuedAt());
    }

    private static Shipment copyOf(Shipment shipment) {
        return Shipment.reconstitute(shipment.getId(), shipment.getOrder(),
                shipment.getOriginWarehouse(), shipment.getDestination(),
                shipment.getTrackingNumber(), shipment.getStatus());
    }

    private static final class CopyingOrderRepository implements OrderRepositoryPort {
        private final Map<String, Order> store = new HashMap<>();

        void seed(Order order) {
            store.put(order.getId(), copyOf(order));
        }

        Order stored(String id) {
            return copyOf(store.get(id));
        }

        @Override
        public Order save(Order order) {
            seed(order);
            return copyOf(store.get(order.getId()));
        }

        @Override
        public Optional<Order> findById(String id) {
            return Optional.ofNullable(store.get(id)).map(OrderServiceCopyingFakeTest::copyOf);
        }

        @Override
        public List<Order> findAll() {
            return store.values().stream().map(OrderServiceCopyingFakeTest::copyOf).toList();
        }

        @Override
        public List<Order> findByBuyerId(String buyerId) {
            return findAll().stream()
                    .filter(order -> order.getBuyer().getId().equals(buyerId))
                    .toList();
        }

        @Override
        public List<Order> findBySellerId(String sellerId) {
            return findAll();
        }

        @Override
        public List<Order> findByStatus(OrderStatus status) {
            return findAll().stream().filter(order -> order.getStatus().equals(status)).toList();
        }

        @Override
        public List<Order> findByBuyerIdAndStatus(String buyerId, OrderStatus status) {
            return findByBuyerId(buyerId).stream()
                    .filter(order -> order.getStatus().equals(status))
                    .toList();
        }

        @Override
        public List<Order> findByPaymentStatusAndCreatedAtBetween(
                PaymentStatus paymentStatus, Instant from, Instant to) {
            return findAll();
        }
    }

    private static final class CopyingInvoiceRepository implements InvoiceRepositoryPort {
        private final Map<String, Invoice> store = new HashMap<>();

        @Override
        public Invoice save(Invoice invoice) {
            store.put(invoice.getId(), copyOf(invoice));
            return copyOf(store.get(invoice.getId()));
        }

        @Override
        public Optional<Invoice> findById(String id) {
            return Optional.ofNullable(store.get(id)).map(OrderServiceCopyingFakeTest::copyOf);
        }

        @Override
        public Optional<Invoice> findByOrderId(String orderId) {
            return store.values().stream()
                    .filter(invoice -> invoice.getOrder().getId().equals(orderId))
                    .findFirst()
                    .map(OrderServiceCopyingFakeTest::copyOf);
        }

        @Override
        public List<Invoice> findByBuyerId(String buyerId) {
            return store.values().stream()
                    .filter(invoice -> invoice.getOrder().getBuyer().getId().equals(buyerId))
                    .map(OrderServiceCopyingFakeTest::copyOf)
                    .toList();
        }

        @Override
        public boolean existsByOrderId(String orderId) {
            return store.values().stream()
                    .anyMatch(invoice -> invoice.getOrder().getId().equals(orderId));
        }
    }

    private static final class CopyingInventoryRepository implements InventoryRepositoryPort {
        private final Map<String, Inventory> store = new HashMap<>();

        void seed(Inventory inventory) {
            store.put(inventory.getId(), copyOf(inventory));
        }

        Inventory stored(String id) {
            return copyOf(store.get(id));
        }

        @Override
        public Inventory save(Inventory inventory) {
            seed(inventory);
            return copyOf(store.get(inventory.getId()));
        }

        @Override
        public Optional<Inventory> findById(String id) {
            return Optional.ofNullable(store.get(id)).map(OrderServiceCopyingFakeTest::copyOf);
        }

        @Override
        public Optional<Inventory> findByProductIdAndWarehouseId(String productId, String warehouseId) {
            return store.values().stream()
                    .filter(inventory -> inventory.getProduct().getId().equals(productId)
                            && inventory.getWarehouse().getId().equals(warehouseId))
                    .findFirst()
                    .map(OrderServiceCopyingFakeTest::copyOf);
        }

        @Override
        public List<Inventory> findByProductId(String productId) {
            return store.values().stream()
                    .filter(inventory -> inventory.getProduct().getId().equals(productId))
                    .map(OrderServiceCopyingFakeTest::copyOf)
                    .toList();
        }

        @Override
        public List<Inventory> findByWarehouseId(String warehouseId) {
            return store.values().stream()
                    .filter(inventory -> inventory.getWarehouse().getId().equals(warehouseId))
                    .map(OrderServiceCopyingFakeTest::copyOf)
                    .toList();
        }

        @Override
        public List<Inventory> findBySellerId(String sellerId) {
            return store.values().stream()
                    .filter(inventory -> inventory.getProduct().getSeller().getId().equals(sellerId))
                    .map(OrderServiceCopyingFakeTest::copyOf)
                    .toList();
        }

        @Override
        public List<Inventory> findAll() {
            return store.values().stream().map(OrderServiceCopyingFakeTest::copyOf).toList();
        }

        @Override
        public boolean existsByProductId(String productId) {
            return store.values().stream()
                    .anyMatch(inventory -> inventory.getProduct().getId().equals(productId));
        }
    }

    private static final class CopyingShipmentRepository implements ShipmentRepositoryPort {
        private final Map<String, Shipment> store = new HashMap<>();

        void seed(Shipment shipment) {
            store.put(shipment.getId(), copyOf(shipment));
        }

        @Override
        public Shipment save(Shipment shipment) {
            seed(shipment);
            return copyOf(store.get(shipment.getId()));
        }

        @Override
        public Optional<Shipment> findById(String id) {
            return Optional.ofNullable(store.get(id)).map(OrderServiceCopyingFakeTest::copyOf);
        }

        @Override
        public List<Shipment> findByOrderId(String orderId) {
            return store.values().stream()
                    .filter(shipment -> shipment.getOrder().getId().equals(orderId))
                    .map(OrderServiceCopyingFakeTest::copyOf)
                    .toList();
        }

        @Override
        public List<Shipment> findByOrderIdAndOriginWarehouseId(String orderId, String warehouseId) {
            return store.values().stream()
                    .filter(shipment -> shipment.getOrder().getId().equals(orderId)
                            && shipment.getOriginWarehouse().getId().equals(warehouseId))
                    .map(OrderServiceCopyingFakeTest::copyOf)
                    .toList();
        }

        @Override
        public List<Shipment> findAll() {
            return store.values().stream().map(OrderServiceCopyingFakeTest::copyOf).toList();
        }
    }

    private static final class RecordingMovementRepository implements InventoryMovementRepositoryPort {
        private final List<InventoryMovement> saved = new ArrayList<>();

        @Override
        public InventoryMovement save(InventoryMovement movement) {
            saved.add(movement);
            return movement;
        }

        @Override
        public List<InventoryMovement> findByInventoryId(String inventoryId) {
            return saved.stream()
                    .filter(movement -> movement.getInventory().getId().equals(inventoryId))
                    .toList();
        }

        @Override
        public List<InventoryMovement> findAll() {
            return List.copyOf(saved);
        }
    }

    private static final class NoopAuditPort implements AuditPort {
        @Override
        public void record(AuditLog auditLog) {
        }

        @Override
        public List<AuditLog> find(AuditAction action, LocalDate from, LocalDate to) {
            return List.of();
        }

        @Override
        public List<AuditLog> findAll() {
            return List.of();
        }
    }

    private static final class FixedClock implements ClockPort {
        private final Instant now;

        private FixedClock(Instant now) {
            this.now = now;
        }

        @Override
        public Instant now() {
            return now;
        }

        @Override
        public LocalDate today() {
            return LocalDate.ofInstant(now, java.time.ZoneOffset.UTC);
        }
    }
}
