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

import application.domain.enums.ShipmentStatus;
import application.domain.exceptions.common.DuplicateEntityException;
import application.domain.exceptions.product.InvalidProductTypeException;
import application.domain.exceptions.warehouse.InvalidWarehouseTypeException;
import application.domain.models.Order;
import application.domain.models.Product;
import application.domain.models.Shipment;
import application.domain.models.Warehouse;
import application.domain.ports.out.AuditPort;
import application.domain.ports.out.ClockPort;
import application.domain.ports.out.OrderRepositoryPort;
import application.domain.ports.out.ShipmentRepositoryPort;
import application.domain.ports.out.WarehouseRepositoryPort;
import application.domain.valueobjects.StockAllocation;
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
class LogisticsServiceTest {
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Mock ShipmentRepositoryPort shipmentRepository;
    @Mock OrderRepositoryPort orderRepository;
    @Mock WarehouseRepositoryPort warehouseRepository;
    @Mock OrderService orderService;
    @Mock AuditPort auditPort;
    @Mock ClockPort clockPort;

    private LogisticsService service;

    @BeforeEach
    void setUp() {
        service = new LogisticsService(shipmentRepository, orderRepository, warehouseRepository,
                orderService, auditPort, clockPort);
        when(clockPort.now()).thenReturn(NOW);
        when(shipmentRepository.save(any(Shipment.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void digitalOrderCannotGenerateShipment() {
        Order order = digitalOrder();
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        assertThatThrownBy(() -> service.createShipment(operator(), order.getId(), "w"))
                .isInstanceOf(InvalidProductTypeException.class);
    }

    @Test
    void warehouseMustBelongToOrderAllocations() {
        Order order = physicalOrder();
        Warehouse other = Warehouse.createMarketplace("Other", address(), operator());
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(warehouseRepository.findById(other.getId())).thenReturn(Optional.of(other));
        assertThatThrownBy(() -> service.createShipment(operator(), order.getId(), other.getId()))
                .isInstanceOf(InvalidWarehouseTypeException.class);
    }

    @Test
    void duplicateShipmentIsRejected() {
        Order order = physicalOrder();
        Warehouse warehouse = order.getItems().get(0).getProduct().getSeller().getWarehouses().get(0);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(warehouseRepository.findById(warehouse.getId())).thenReturn(Optional.of(warehouse));
        when(shipmentRepository.findByOrderIdAndOriginWarehouseId(order.getId(), warehouse.getId()))
                .thenReturn(List.of(Shipment.create(order, warehouse)));
        assertThatThrownBy(() -> service.createShipment(operator(), order.getId(), warehouse.getId()))
                .isInstanceOf(DuplicateEntityException.class);
    }

    @Test
    void createShipmentPersistsAndAudits() {
        Order order = physicalOrder();
        Warehouse warehouse = order.getItems().get(0).getProduct().getSeller().getWarehouses().get(0);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(warehouseRepository.findById(warehouse.getId())).thenReturn(Optional.of(warehouse));
        when(shipmentRepository.findByOrderIdAndOriginWarehouseId(order.getId(), warehouse.getId()))
                .thenReturn(List.of());
        Shipment shipment = service.createShipment(operator(), order.getId(), warehouse.getId());
        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.PENDING);
        verify(auditPort).record(any());
    }

    @Test
    void startShippingOnlyDispatchesWhenAllShipmentsMoved() {
        Order order = physicalOrder();
        Warehouse warehouse = order.getItems().get(0).getProduct().getSeller().getWarehouses().get(0);
        Shipment shipment = Shipment.create(order, warehouse);
        when(shipmentRepository.findById(shipment.getId())).thenReturn(Optional.of(shipment));
        when(shipmentRepository.findByOrderId(order.getId())).thenReturn(List.of(shipment));
        application.domain.models.User logisticsActor = operator();
        service.startShipping(logisticsActor, shipment.getId(), "TRACK");
        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.IN_TRANSIT);
        verify(orderService).markDispatched(logisticsActor, order.getId());
    }

    @Test
    void buyerCannotReadAnotherBuyerShipment() {
        Order order = physicalOrder();
        Warehouse warehouse = order.getItems().get(0).getProduct().getSeller().getWarehouses().get(0);
        Shipment shipment = Shipment.create(order, warehouse);
        when(shipmentRepository.findById(shipment.getId())).thenReturn(Optional.of(shipment));
        assertThatThrownBy(() -> service.getShipment(buyer().getUser(), shipment.getId()))
                .isInstanceOf(application.domain.exceptions.common.UnauthorizedOperationException.class);
    }

    private Order physicalOrder() {
        Product product = publishedPhysicalProduct(seller(admin()), operator());
        Order order = Order.fromCart(cartWith(buyer(), product, 1), address(), NOW);
        order.assignAllocations(List.of(StockAllocation.of(product.getId(),
                product.getSeller().getWarehouses().get(0).getId(), 1)));
        return order;
    }

    private Order digitalOrder() {
        Product product = publishedDigitalProduct(seller(admin()));
        return Order.fromCart(cartWith(buyer(), product, 1), address(), NOW);
    }
}
