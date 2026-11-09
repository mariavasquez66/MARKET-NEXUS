package application.adapters.rest.controllers;

import application.adapters.rest.mappers.InventoryMovementRestMapper;
import application.adapters.rest.mappers.InventoryRestMapper;
import application.adapters.rest.mappers.OrderRestMapper;
import application.adapters.rest.mappers.ReturnRestMapper;
import application.adapters.rest.mappers.ShipmentRestMapper;
import application.domain.DomainFixtures;
import application.domain.enums.MovementType;
import application.domain.enums.OrderStatus;
import application.domain.enums.UserRole;
import application.domain.exceptions.logistics.ShipmentAlreadyStartedException;
import application.domain.exceptions.logistics.ShipmentNotFoundException;
import application.domain.models.Buyer;
import application.domain.models.Cart;
import application.domain.models.Inventory;
import application.domain.models.InventoryMovement;
import application.domain.models.Order;
import application.domain.models.Product;
import application.domain.models.Return;
import application.domain.models.Seller;
import application.domain.models.Shipment;
import application.domain.models.User;
import application.domain.models.Warehouse;
import application.domain.ports.in.BusinessOperatorPort;
import application.domain.valueobjects.StockAllocation;
import application.infrastructure.exception.GlobalExceptionHandler;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static application.adapters.rest.controllers.RestControllerTestSupport.actor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LogisticsControllerTest {

    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");

    private BusinessOperatorPort operatorPort;
    private MockMvc mockMvc;
    private User actor;

    @BeforeEach
    void setUp() {
        operatorPort = mock(BusinessOperatorPort.class);
        actor = DomainFixtures.operator();
        LogisticsController controller = new LogisticsController(
                operatorPort,
                new OrderRestMapper(),
                new ShipmentRestMapper(),
                new InventoryMovementRestMapper(),
                new InventoryRestMapper(),
                new ReturnRestMapper());
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(RestControllerTestSupport.actorResolver())
                .build();
    }

    @Test
    void listOrdersWithoutStatusReturns200() throws Exception {
        Order order = order();
        when(operatorPort.listOrders(actor, null)).thenReturn(List.of(order));

        mockMvc.perform(get("/api/v1/logistics/orders").with(actor(actor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(order.getId()))
                .andExpect(jsonPath("$[0].status").value("PENDING_PAYMENT"));
    }

    @Test
    void listOrdersWithStatusReturns200() throws Exception {
        Order order = order();
        when(operatorPort.listOrders(actor, OrderStatus.PAID)).thenReturn(List.of(order));

        mockMvc.perform(get("/api/v1/logistics/orders")
                        .param("status", "PAID")
                        .with(actor(actor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(order.getId()));
    }

    @Test
    void listOrdersRejectsInvalidStatus() throws Exception {
        mockMvc.perform(get("/api/v1/logistics/orders")
                        .param("status", "NOPE")
                        .with(actor(actor)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DATA"));
    }

    @Test
    void getShipmentReturns200() throws Exception {
        Shipment shipment = shipment();
        when(operatorPort.getShipment(actor, shipment.getId())).thenReturn(shipment);

        mockMvc.perform(get("/api/v1/logistics/shipments/{id}", shipment.getId())
                        .with(actor(actor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(shipment.getId()))
                .andExpect(jsonPath("$.orderId").value(shipment.getOrder().getId()))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void listShipmentsByOrderReturns200() throws Exception {
        Shipment shipment = shipment();
        when(operatorPort.listShipments(actor, shipment.getOrder().getId()))
                .thenReturn(List.of(shipment));

        mockMvc.perform(get("/api/v1/logistics/orders/{id}/shipments",
                        shipment.getOrder().getId())
                        .with(actor(actor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(shipment.getId()))
                .andExpect(jsonPath("$[0].orderId").value(shipment.getOrder().getId()));
    }

    @Test
    void createShipmentReturns201() throws Exception {
        Shipment shipment = shipment();
        when(operatorPort.createShipment(any(), any(), any())).thenReturn(shipment);

        mockMvc.perform(post("/api/v1/logistics/shipments")
                        .with(actor(actor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId":"o1","warehouseId":"w1"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(shipment.getId()))
                .andExpect(jsonPath("$.orderId").value(shipment.getOrder().getId()))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void createShipmentRejectsBlankOrderId() throws Exception {
        mockMvc.perform(post("/api/v1/logistics/shipments")
                        .with(actor(actor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId":"","warehouseId":"w1"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void createShipmentReturns404WhenOrderMissing() throws Exception {
        when(operatorPort.createShipment(any(), any(), any()))
                .thenThrow(new ShipmentNotFoundException("missing"));

        mockMvc.perform(post("/api/v1/logistics/shipments")
                        .with(actor(actor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderId":"o1","warehouseId":"w1"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void startShipmentReturns200() throws Exception {
        Shipment shipment = shipment();
        shipment.startShipping("TRK-001");
        when(operatorPort.startShipping(any(), any(), any())).thenReturn(shipment);

        mockMvc.perform(post("/api/v1/logistics/shipments/{id}/start", shipment.getId())
                        .with(actor(actor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"trackingNumber":"TRK-001"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackingNumber").value("TRK-001"))
                .andExpect(jsonPath("$.status").value("IN_TRANSIT"));
    }

    @Test
    void startShipmentReturns409WhenAlreadyStarted() throws Exception {
        when(operatorPort.startShipping(any(), any(), any()))
                .thenThrow(new ShipmentAlreadyStartedException("already started"));

        mockMvc.perform(post("/api/v1/logistics/shipments/{id}/start", "s1")
                        .with(actor(actor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"trackingNumber":"TRK-001"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SHIPMENT_ALREADY_STARTED"));
    }

    @Test
    void deliverShipmentReturns200() throws Exception {
        Shipment shipment = shipment();
        shipment.startShipping("TRK-001");
        shipment.confirmDelivery();
        when(operatorPort.confirmDelivery(any(), any())).thenReturn(shipment);

        mockMvc.perform(post("/api/v1/logistics/shipments/{id}/deliver", shipment.getId())
                        .with(actor(actor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"));
    }

    @Test
    void registerInflowReturns200() throws Exception {
        InventoryMovement movement = movement(MovementType.INFLOW, 5);
        when(operatorPort.registerInflow(any(), any(), any(), anyInt())).thenReturn(movement);

        mockMvc.perform(post("/api/v1/logistics/inventory/{productId}/{warehouseId}/inflow",
                                "p1", "w1")
                        .with(actor(actor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantity":5}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(movement.getId()))
                .andExpect(jsonPath("$.type").value("INFLOW"))
                .andExpect(jsonPath("$.quantity").value(5));
    }

    @Test
    void registerInflowRejectsMissingQuantity() throws Exception {
        mockMvc.perform(post("/api/v1/logistics/inventory/{productId}/{warehouseId}/inflow",
                                "p1", "w1")
                        .with(actor(actor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DATA"))
                .andExpect(jsonPath("$.message").value("quantity is required"));
    }

    @Test
    void adjustInventoryReturns200() throws Exception {
        InventoryMovement movement = movement(MovementType.ADJUSTMENT, 3);
        when(operatorPort.adjustInventory(any(), any(), any(), anyInt())).thenReturn(movement);

        mockMvc.perform(post("/api/v1/logistics/inventory/{productId}/{warehouseId}/adjust",
                                "p1", "w1")
                        .with(actor(actor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"delta":3}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("ADJUSTMENT"))
                .andExpect(jsonPath("$.quantity").value(3));
    }

    @Test
    void adjustInventoryRejectsMissingDelta() throws Exception {
        mockMvc.perform(post("/api/v1/logistics/inventory/{productId}/{warehouseId}/adjust",
                                "p1", "w1")
                        .with(actor(actor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DATA"))
                .andExpect(jsonPath("$.message").value("delta is required"));
    }

    @Test
    void markDamagedReturns200() throws Exception {
        Inventory inventory = damagedInventory();
        when(operatorPort.markDamaged(any(), eq("p1"), eq("w1"))).thenReturn(inventory);

        mockMvc.perform(post("/api/v1/logistics/inventory/{productId}/{warehouseId}/damaged",
                                "p1", "w1")
                        .with(actor(actor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(inventory.getId()))
                .andExpect(jsonPath("$.condition").value("DAMAGED"));
    }

    @Test
    void markRepairedReturns200() throws Exception {
        Inventory inventory = repairedInventory();
        when(operatorPort.markRepaired(any(), eq("p1"), eq("w1"))).thenReturn(inventory);

        mockMvc.perform(post("/api/v1/logistics/inventory/{productId}/{warehouseId}/repaired",
                                "p1", "w1")
                        .with(actor(actor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(inventory.getId()))
                .andExpect(jsonPath("$.condition").value("GOOD"));
    }

    @Test
    void listReturnsReturns200() throws Exception {
        Return returnRequest = requestedReturn();
        when(operatorPort.listReturns(actor)).thenReturn(List.of(returnRequest));

        mockMvc.perform(get("/api/v1/logistics/returns").with(actor(actor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(returnRequest.getId()))
                .andExpect(jsonPath("$[0].status").value("REQUESTED"));
    }

    @Test
    void receiveReturnReturns200() throws Exception {
        Return returnRequest = completedReturn();
        when(operatorPort.receiveReturn(any(), eq("r1"))).thenReturn(returnRequest);

        mockMvc.perform(post("/api/v1/logistics/returns/{id}/receive", "r1")
                        .with(actor(actor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(returnRequest.getId()))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    private static Product product() {
        Seller seller = DomainFixtures.seller(DomainFixtures.admin());
        return DomainFixtures.publishedPhysicalProduct(seller, DomainFixtures.operator());
    }

    private static Order orderFor(Product product) {
        Buyer buyer = DomainFixtures.buyer();
        Cart cart = DomainFixtures.cartWith(buyer, product, 2);
        return Order.fromCart(cart, DomainFixtures.address(), NOW);
    }

    private static Order order() {
        return orderFor(product());
    }

    private static Order deliveredOrder() {
        Product product = product();
        Warehouse warehouse = product.getSeller().getWarehouses().get(0);
        Order order = orderFor(product);
        order.assignAllocations(List.of(StockAllocation.of(product.getId(), warehouse.getId(), 2)));
        order.confirmPayment(NOW);
        order.markDispatched();
        order.markDelivered();
        return order;
    }

    private static Shipment shipment() {
        Product product = product();
        Warehouse warehouse = product.getSeller().getWarehouses().get(0);
        return Shipment.create(orderFor(product), warehouse);
    }

    private static Inventory inventory() {
        Product product = product();
        Warehouse warehouse = product.getSeller().getWarehouses().get(0);
        return Inventory.create(product, warehouse, 10);
    }

    private static Inventory damagedInventory() {
        Inventory inventory = inventory();
        inventory.markDamaged();
        return inventory;
    }

    private static Inventory repairedInventory() {
        Inventory inventory = damagedInventory();
        inventory.markRepaired();
        return inventory;
    }

    private static InventoryMovement movement(MovementType type, int quantity) {
        return InventoryMovement.create(inventory(), type, quantity, DomainFixtures.operator(), NOW);
    }

    private static Return requestedReturn() {
        return Return.request(deliveredOrder(), "Broken item", NOW);
    }

    private static Return completedReturn() {
        Return returnRequest = requestedReturn();
        returnRequest.approve();
        returnRequest.complete();
        return returnRequest;
    }
}
