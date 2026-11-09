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
import application.domain.exceptions.order.InvalidStateTransitionException;
import application.domain.models.Buyer;
import application.domain.models.Order;
import application.domain.models.Product;
import application.domain.models.Shipment;
import application.domain.models.Warehouse;
import application.domain.ports.out.AuditPort;
import application.domain.ports.out.ClockPort;
import application.domain.ports.out.OrderRepositoryPort;
import application.domain.ports.out.ShipmentRepositoryPort;
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
class OrderFulfillmentServiceTest {
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Mock OrderRepositoryPort orderRepository;
    @Mock ShipmentRepositoryPort shipmentRepository;
    @Mock AuditPort auditPort;
    @Mock ClockPort clockPort;

    private OrderFulfillmentService service;

    @BeforeEach
    void setUp() {
        service = new OrderFulfillmentService(orderRepository, shipmentRepository, auditPort, clockPort);
        when(clockPort.now()).thenReturn(NOW);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void dispatchRequiresEveryShipmentInTransit() {
        Buyer buyer = buyer();
        Product product = publishedPhysicalProduct(seller(admin()), operator());
        Warehouse warehouse = product.getSeller().getWarehouses().get(0);
        Order order = Order.fromCart(cartWith(buyer, product, 1), address(), NOW);
        order.confirmPayment(NOW);
        order.assignAllocations(List.of(StockAllocation.of(product.getId(), warehouse.getId(), 1)));
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        Shipment inTransit = Shipment.create(order, warehouse);
        inTransit.startShipping("TRACK-1");
        Shipment pending = Shipment.create(order, warehouse);
        when(shipmentRepository.findByOrderIdAndOriginWarehouseId(order.getId(), warehouse.getId()))
                .thenReturn(List.of(inTransit, pending));

        assertThatThrownBy(() -> service.markDispatched(operator(), order.getId()))
                .isInstanceOf(InvalidStateTransitionException.class);

        when(shipmentRepository.findByOrderIdAndOriginWarehouseId(order.getId(), warehouse.getId()))
                .thenReturn(List.of(inTransit));
        assertThat(service.markDispatched(operator(), order.getId()).getStatus())
                .isEqualTo(OrderStatus.DISPATCHED);
    }

    @Test
    void deliveryRequiresEveryShipmentDelivered() {
        Buyer buyer = buyer();
        Product product = publishedPhysicalProduct(seller(admin()), operator());
        Warehouse warehouse = product.getSeller().getWarehouses().get(0);
        Order order = Order.fromCart(cartWith(buyer, product, 1), address(), NOW);
        order.confirmPayment(NOW);
        order.assignAllocations(List.of(StockAllocation.of(product.getId(), warehouse.getId(), 1)));
        order.markDispatched();
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        Shipment delivered = Shipment.create(order, warehouse);
        delivered.startShipping("TRACK-1");
        delivered.confirmDelivery();
        Shipment inTransit = Shipment.create(order, warehouse);
        inTransit.startShipping("TRACK-2");
        when(shipmentRepository.findByOrderIdAndOriginWarehouseId(order.getId(), warehouse.getId()))
                .thenReturn(List.of(delivered, inTransit));

        assertThatThrownBy(() -> service.markDelivered(operator(), order.getId()))
                .isInstanceOf(InvalidStateTransitionException.class);

        when(shipmentRepository.findByOrderIdAndOriginWarehouseId(order.getId(), warehouse.getId()))
                .thenReturn(List.of(delivered));
        assertThat(service.markDelivered(operator(), order.getId()).getStatus())
                .isEqualTo(OrderStatus.DELIVERED);
    }
}
