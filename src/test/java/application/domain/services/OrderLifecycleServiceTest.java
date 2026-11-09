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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import application.domain.enums.MovementType;
import application.domain.enums.OrderStatus;
import application.domain.exceptions.common.UnauthorizedOperationException;
import application.domain.models.Buyer;
import application.domain.models.Inventory;
import application.domain.models.InventoryMovement;
import application.domain.models.Order;
import application.domain.models.Product;
import application.domain.models.Warehouse;
import application.domain.ports.out.AuditPort;
import application.domain.ports.out.ClockPort;
import application.domain.ports.out.InventoryMovementRepositoryPort;
import application.domain.ports.out.InventoryRepositoryPort;
import application.domain.ports.out.OrderRepositoryPort;
import application.domain.valueobjects.StockAllocation;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderLifecycleServiceTest {
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Mock OrderRepositoryPort orderRepository;
    @Mock InventoryRepositoryPort inventoryRepository;
    @Mock InventoryMovementRepositoryPort movementRepository;
    @Mock AuditPort auditPort;
    @Mock ClockPort clockPort;

    private OrderLifecycleService service;

    @BeforeEach
    void setUp() {
        service = new OrderLifecycleService(orderRepository, inventoryRepository, movementRepository,
                auditPort, clockPort);
        when(clockPort.now()).thenReturn(NOW);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryRepository.save(any(Inventory.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void cancelReleasesReservationsAndRejectsNonOwner() {
        Buyer buyer = buyer();
        Product product = publishedPhysicalProduct(seller(admin()), operator());
        Warehouse warehouse = product.getSeller().getWarehouses().get(0);
        Order order = Order.fromCart(cartWith(buyer, product, 2), address(), NOW);
        order.assignAllocations(List.of(StockAllocation.of(product.getId(), warehouse.getId(), 2)));
        Inventory inventory = Inventory.create(product, warehouse, 5);
        inventory.reserve(2);
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(inventoryRepository.findByProductIdAndWarehouseId(product.getId(), warehouse.getId()))
                .thenReturn(Optional.of(inventory));

        assertThatThrownBy(() -> service.cancel(buyer().getUser(), order.getId()))
                .isInstanceOf(UnauthorizedOperationException.class);
        Order cancelled = service.cancel(buyer.getUser(), order.getId());

        assertThat(cancelled.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(inventory.getAvailableQuantity()).isEqualTo(5);
        assertThat(inventory.getReservedQuantity()).isZero();
        ArgumentCaptor<InventoryMovement> movement = ArgumentCaptor.forClass(InventoryMovement.class);
        verify(movementRepository).save(movement.capture());
        assertThat(movement.getValue().getType()).isEqualTo(MovementType.RESERVATION);
        assertThat(movement.getValue().getQuantity()).isEqualTo(2);
    }

    @Test
    void finalizeMovesDeliveredOrderToFinalized() {
        Buyer buyer = buyer();
        Product product = publishedPhysicalProduct(seller(admin()), operator());
        Warehouse warehouse = product.getSeller().getWarehouses().get(0);
        Order order = Order.fromCart(cartWith(buyer, product, 1), address(), NOW);
        order.assignAllocations(List.of(StockAllocation.of(product.getId(), warehouse.getId(), 1)));
        order.confirmPayment(NOW);
        order.markDispatched();
        order.markDelivered();
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        Order finalized = service.finalize(buyer.getUser(), order.getId());

        assertThat(finalized.getStatus()).isEqualTo(OrderStatus.FINALIZED);
    }
}
