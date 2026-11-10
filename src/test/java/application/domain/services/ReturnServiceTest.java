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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import application.domain.enums.ReturnStatus;
import application.domain.exceptions.common.UnauthorizedOperationException;
import application.domain.models.Cart;
import application.domain.models.Order;
import application.domain.models.Product;
import application.domain.models.Return;
import application.domain.models.User;
import application.domain.ports.out.AuditPort;
import application.domain.ports.out.ClockPort;
import application.domain.ports.out.OrderRepositoryPort;
import application.domain.ports.out.ReturnRepositoryPort;
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
class ReturnServiceTest {
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Mock ReturnRepositoryPort returnRepository;
    @Mock OrderRepositoryPort orderRepository;
    @Mock InventoryService inventoryService;
    @Mock AuditPort auditPort;
    @Mock ClockPort clockPort;

    private ReturnService service;

    @BeforeEach
    void setUp() {
        service = new ReturnService(returnRepository, orderRepository, inventoryService,
                auditPort, clockPort);
        when(clockPort.now()).thenReturn(NOW);
        when(returnRepository.save(any(Return.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void onlyOrderOwnerCanRequestReturn() {
        Order order = deliveredOrder();
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        assertThatThrownBy(() -> service.requestReturn(buyer().getUser(), order.getId(), "Broken"))
                .isInstanceOf(UnauthorizedOperationException.class);
        Return returnRequest = service.requestReturn(order.getBuyer().getUser(), order.getId(), "Broken");
        assertThat(returnRequest.getStatus()).isEqualTo(ReturnStatus.REQUESTED);
    }

    @Test
    void onlyAdminCanApproveOrReject() {
        Order order = deliveredOrder();
        Return returnRequest = Return.request(order, "Broken", NOW);
        when(returnRepository.findById(returnRequest.getId())).thenReturn(Optional.of(returnRequest));
        assertThatThrownBy(() -> service.approve(order.getBuyer().getUser(), returnRequest.getId()))
                .isInstanceOf(application.domain.exceptions.common.InsufficientPermissionException.class);
        service.approve(admin(), returnRequest.getId());
        assertThat(returnRequest.getStatus()).isEqualTo(ReturnStatus.APPROVED);
    }

    @Test
    void completingReturnRegistersPhysicalStock() {
        Order order = deliveredOrder();
        Return returnRequest = Return.request(order, "Broken", NOW);
        returnRequest.approve();
        when(returnRepository.findById(returnRequest.getId())).thenReturn(Optional.of(returnRequest));
        application.domain.models.User logisticsActor = operator();
        Return completed = service.complete(logisticsActor, returnRequest.getId());
        assertThat(completed.getStatus()).isEqualTo(ReturnStatus.COMPLETED);
        verify(inventoryService).registerReturn(logisticsActor,
                order.getAllocations().get(0).getProductId(),
                order.getAllocations().get(0).getWarehouseId(),
                order.getAllocations().get(0).getQuantity());
    }

    @Test
    void completingPartialReturnRegistersOnlyRequestedProduct() {
        User admin = admin();
        User logisticsActor = operator();
        Product returned = publishedPhysicalProduct(seller(admin), logisticsActor);
        Product untouched = publishedPhysicalProduct(seller(admin), logisticsActor);
        Cart cart = Cart.create(buyer());
        cart.addItem(returned, 1);
        cart.addItem(untouched, 1);
        Order order = Order.fromCart(cart, address(), NOW);
        String returnedWarehouse = returned.getSeller().getWarehouses().get(0).getId();
        String untouchedWarehouse = untouched.getSeller().getWarehouses().get(0).getId();
        order.assignAllocations(List.of(
                StockAllocation.of(returned.getId(), returnedWarehouse, 1),
                StockAllocation.of(untouched.getId(), untouchedWarehouse, 1)));
        order.confirmPayment(NOW);
        order.markDispatched();
        order.markDelivered();

        Return returnRequest = Return.request(order, "Broken", NOW, returned.getId(), 1);
        returnRequest.approve();
        when(returnRepository.findById(returnRequest.getId())).thenReturn(Optional.of(returnRequest));

        Return completed = service.complete(logisticsActor, returnRequest.getId());

        assertThat(completed.getStatus()).isEqualTo(ReturnStatus.COMPLETED);
        verify(inventoryService).registerReturn(logisticsActor, returned.getId(), returnedWarehouse, 1);
        verify(inventoryService, never()).registerReturn(logisticsActor, untouched.getId(),
                untouchedWarehouse, 1);
    }

    @Test
    void buyerOnlySeesOwnReturns() {
        Order order = deliveredOrder();
        Return returnRequest = Return.request(order, "Broken", NOW);
        when(returnRepository.findAll()).thenReturn(List.of(returnRequest));
        assertThat(service.listReturns(order.getBuyer().getUser())).containsExactly(returnRequest);
        assertThat(service.listReturns(buyer().getUser())).isEmpty();
    }

    private Order deliveredOrder() {
        Product product = publishedPhysicalProduct(seller(admin()), operator());
        Order order = Order.fromCart(cartWith(orderBuyer(), product, 1), address(), NOW);
        order.assignAllocations(List.of(StockAllocation.of(product.getId(),
                product.getSeller().getWarehouses().get(0).getId(), 1)));
        order.confirmPayment(NOW);
        order.markDispatched();
        order.markDelivered();
        return order;
    }

    private application.domain.models.Buyer orderBuyer() {
        return buyer();
    }
}
