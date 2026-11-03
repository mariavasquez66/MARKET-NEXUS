package application.adapters.rest.mappers;

import application.adapters.rest.dto.request.AddressRequest;
import application.adapters.rest.dto.response.AddressResponse;
import application.adapters.rest.dto.response.AuditLogResponse;
import application.adapters.rest.dto.response.BuyerResponse;
import application.adapters.rest.dto.response.CartItemResponse;
import application.adapters.rest.dto.response.CartResponse;
import application.adapters.rest.dto.response.InventoryMovementResponse;
import application.adapters.rest.dto.response.InventoryResponse;
import application.adapters.rest.dto.response.InventorySummaryResponse;
import application.adapters.rest.dto.response.InvoiceResponse;
import application.adapters.rest.dto.response.MoneyResponse;
import application.adapters.rest.dto.response.OrderItemResponse;
import application.adapters.rest.dto.response.OrderResponse;
import application.adapters.rest.dto.response.OrderSummaryResponse;
import application.adapters.rest.dto.response.ProductResponse;
import application.adapters.rest.dto.response.ProductVariantResponse;
import application.adapters.rest.dto.response.RefundResponse;
import application.adapters.rest.dto.response.ReturnResponse;
import application.adapters.rest.dto.response.ReturnSummaryResponse;
import application.adapters.rest.dto.response.SalesReportResponse;
import application.adapters.rest.dto.response.SellerResponse;
import application.adapters.rest.dto.response.ShipmentResponse;
import application.adapters.rest.dto.response.UserResponse;
import application.adapters.rest.dto.response.UserSummaryResponse;
import application.adapters.rest.dto.response.WarehouseResponse;
import application.domain.DomainFixtures;
import application.domain.enums.AuditAction;
import application.domain.enums.AuditEntityType;
import application.domain.enums.MovementType;
import application.domain.enums.UserRole;
import application.domain.enums.VariantType;
import application.domain.models.AuditLog;
import application.domain.models.Buyer;
import application.domain.models.Cart;
import application.domain.models.Inventory;
import application.domain.models.InventoryMovement;
import application.domain.models.Invoice;
import application.domain.models.Order;
import application.domain.models.Product;
import application.domain.models.Refund;
import application.domain.models.Return;
import application.domain.models.Seller;
import application.domain.models.Shipment;
import application.domain.models.User;
import application.domain.models.Warehouse;
import application.domain.models.reports.InventorySummary;
import application.domain.models.reports.OrderSummary;
import application.domain.models.reports.ReturnSummary;
import application.domain.models.reports.SalesReport;
import application.domain.models.reports.UserSummary;
import application.domain.valueobjects.Address;
import application.domain.valueobjects.Money;
import application.domain.valueobjects.ProductVariant;
import application.domain.valueobjects.StockAllocation;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RestMappersTest {

    private static final Instant REQUESTED_AT = Instant.parse("2026-01-15T10:00:00Z");

    private final UserRestMapper userMapper = new UserRestMapper();
    private final BuyerRestMapper buyerMapper = new BuyerRestMapper();
    private final SellerRestMapper sellerMapper = new SellerRestMapper();
    private final WarehouseRestMapper warehouseMapper = new WarehouseRestMapper();
    private final ProductRestMapper productMapper = new ProductRestMapper();
    private final InventoryRestMapper inventoryMapper = new InventoryRestMapper();
    private final InventoryMovementRestMapper movementMapper = new InventoryMovementRestMapper();
    private final CartRestMapper cartMapper = new CartRestMapper();
    private final OrderRestMapper orderMapper = new OrderRestMapper();
    private final InvoiceRestMapper invoiceMapper = new InvoiceRestMapper();
    private final ShipmentRestMapper shipmentMapper = new ShipmentRestMapper();
    private final ReturnRestMapper returnMapper = new ReturnRestMapper();
    private final RefundRestMapper refundMapper = new RefundRestMapper();
    private final ReportRestMapper reportMapper = new ReportRestMapper();
    private final AuditLogRestMapper auditLogMapper = new AuditLogRestMapper();

    @Test
    void userMapperNeverExposesPasswordHash() {
        User user = DomainFixtures.user(UserRole.ADMIN);

        UserResponse response = userMapper.toResponse(user);

        assertThat(response.id()).isEqualTo(user.getId());
        assertThat(response.fullName()).isEqualTo(user.getFullName().getValue());
        assertThat(response.email()).isEqualTo(user.getEmail().getValue());
        assertThat(response.identityDocument()).isEqualTo(user.getIdentityDocument().getValue());
        assertThat(response.role()).isEqualTo("ADMIN");
        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(response.authTokenVersion()).isEqualTo(user.getAuthTokenVersion());

        Set<String> components = Arrays.stream(UserResponse.class.getRecordComponents())
                .map(component -> component.getName())
                .collect(Collectors.toSet());
        assertThat(components)
                .containsExactlyInAnyOrder("id", "fullName", "email", "identityDocument",
                        "role", "status", "authTokenVersion")
                .doesNotContain("passwordHash");
        assertThat(response.toString()).doesNotContain(user.getPasswordHash().getValue());
    }

    @Test
    void buyerMapperFlattensAddressesAndCatalogCode() {
        Buyer buyer = DomainFixtures.buyer();
        buyer.addAdditionalAddress(Address.of("Calle 9", "Medellin", "Antioquia", null, "Colombia"));

        BuyerResponse response = buyerMapper.toResponse(buyer);

        assertThat(response.id()).isEqualTo(buyer.getId());
        assertThat(response.userId()).isEqualTo(buyer.getUser().getId());
        assertThat(response.fullName()).isEqualTo(buyer.getUser().getFullName().getValue());
        assertThat(response.email()).isEqualTo(buyer.getUser().getEmail().getValue());
        assertThat(response.commercialStatus()).isEqualTo("ELIGIBLE");
        assertThat(response.mainAddress().street()).isEqualTo("Calle 1 # 2-3");
        assertThat(response.additionalAddresses()).hasSize(1);
        assertThat(response.additionalAddresses().get(0).city()).isEqualTo("Medellin");
    }

    @Test
    void sellerMapperExposesOnlyWarehouseIds() {
        User admin = DomainFixtures.admin();
        Seller seller = DomainFixtures.seller(admin);
        Warehouse warehouse = DomainFixtures.sellerWarehouse(seller, DomainFixtures.operator());

        SellerResponse response = sellerMapper.toResponse(seller);

        assertThat(response.id()).isEqualTo(seller.getId());
        assertThat(response.userId()).isEqualTo(seller.getUser().getId());
        assertThat(response.taxId()).isEqualTo(seller.getTaxId().getValue());
        assertThat(response.warehouseIds()).containsExactly(warehouse.getId());
    }

    @Test
    void warehouseMapperFlattensLocationAndOwner() {
        User admin = DomainFixtures.admin();
        Seller seller = DomainFixtures.seller(admin);
        User responsible = DomainFixtures.operator();
        Warehouse warehouse = DomainFixtures.sellerWarehouse(seller, responsible);

        WarehouseResponse response = warehouseMapper.toResponse(warehouse);

        assertThat(response.id()).isEqualTo(warehouse.getId());
        assertThat(response.name()).isEqualTo(warehouse.getName());
        assertThat(response.type()).isEqualTo("SELLER");
        assertThat(response.ownerSellerId()).isEqualTo(seller.getId());
        assertThat(response.responsibleUserId()).isEqualTo(responsible.getId());
        assertThat(response.location().city()).isEqualTo("Bogota");
    }

    @Test
    void productMapperMapsVariantsAndMoney() {
        Seller seller = DomainFixtures.seller(DomainFixtures.admin());
        Product product = DomainFixtures.publishedPhysicalProduct(seller, DomainFixtures.operator());
        product.addVariant(ProductVariant.of(VariantType.COLOR, "Red"));

        ProductResponse response = productMapper.toResponse(product);

        assertThat(response.id()).isEqualTo(product.getId());
        assertThat(response.sellerId()).isEqualTo(seller.getId());
        assertThat(response.type()).isEqualTo("PHYSICAL");
        assertThat(response.status()).isEqualTo("PUBLISHED");
        assertThat(response.price().currency()).isEqualTo("COP");
        assertThat(response.price().amount()).isEqualByComparingTo("119.00");
        assertThat(response.price().amount().scale()).isEqualTo(2);
        assertThat(response.variants()).containsExactly(new ProductVariantResponse("COLOR", "Red"));
    }

    @Test
    void inventoryMappersMapCatalogCodesAndMovement() {
        Seller seller = DomainFixtures.seller(DomainFixtures.admin());
        User responsible = DomainFixtures.operator();
        Product product = DomainFixtures.publishedPhysicalProduct(seller, responsible);
        Warehouse warehouse = seller.getWarehouses().get(0);
        Inventory inventory = Inventory.create(product, warehouse, 10);
        inventory.reserve(3);

        InventoryResponse inventoryResponse = inventoryMapper.toResponse(inventory);
        assertThat(inventoryResponse.id()).isEqualTo(inventory.getId());
        assertThat(inventoryResponse.productId()).isEqualTo(product.getId());
        assertThat(inventoryResponse.warehouseId()).isEqualTo(warehouse.getId());
        assertThat(inventoryResponse.availableQuantity()).isEqualTo(7);
        assertThat(inventoryResponse.reservedQuantity()).isEqualTo(3);
        assertThat(inventoryResponse.condition()).isEqualTo("GOOD");

        InventoryMovement movement = InventoryMovement.create(
                inventory, MovementType.INFLOW, 10, responsible, REQUESTED_AT);
        InventoryMovementResponse movementResponse = movementMapper.toResponse(movement);
        assertThat(movementResponse.id()).isEqualTo(movement.getId());
        assertThat(movementResponse.inventoryId()).isEqualTo(inventory.getId());
        assertThat(movementResponse.type()).isEqualTo("INFLOW");
        assertThat(movementResponse.quantity()).isEqualTo(10);
        assertThat(movementResponse.performedByUserId()).isEqualTo(responsible.getId());
        assertThat(movementResponse.occurredAt()).isEqualTo(REQUESTED_AT);
    }

    @Test
    void cartMapperComputesSubtotalsAndTotal() {
        Buyer buyer = DomainFixtures.buyer();
        Seller seller = DomainFixtures.seller(DomainFixtures.admin());
        Product product = DomainFixtures.publishedPhysicalProduct(seller, DomainFixtures.operator());
        Cart cart = DomainFixtures.cartWith(buyer, product, 2);

        CartResponse response = cartMapper.toResponse(cart);

        assertThat(response.id()).isEqualTo(cart.getId());
        assertThat(response.buyerId()).isEqualTo(buyer.getId());
        assertThat(response.items()).hasSize(1);
        CartItemResponse item = response.items().get(0);
        assertThat(item.productId()).isEqualTo(product.getId());
        assertThat(item.productName()).isEqualTo(product.getName());
        assertThat(item.quantity()).isEqualTo(2);
        assertThat(item.unitPrice().amount()).isEqualByComparingTo("119.00");
        assertThat(item.subtotal().amount()).isEqualByComparingTo("238.00");
        assertThat(item.subtotal().amount().scale()).isEqualTo(2);
        assertThat(response.total().amount()).isEqualByComparingTo("238.00");
        assertThat(response.total().currency()).isEqualTo("COP");
    }

    @Test
    void orderMapperMapsItemsAllocationsAndInvoiceId() {
        Seller seller = DomainFixtures.seller(DomainFixtures.admin());
        Product product = DomainFixtures.publishedPhysicalProduct(seller, DomainFixtures.operator());
        Warehouse warehouse = seller.getWarehouses().get(0);
        Order order = deliveredOrder(product, warehouse.getId());

        OrderResponse response = orderMapper.toResponse(order);

        assertThat(response.id()).isEqualTo(order.getId());
        assertThat(response.buyerId()).isEqualTo(order.getBuyer().getId());
        assertThat(response.status()).isEqualTo("DELIVERED");
        assertThat(response.paymentStatus()).isEqualTo("CONFIRMED");
        assertThat(response.totalAmount().amount()).isEqualByComparingTo("238.00");
        assertThat(response.items()).hasSize(1);
        OrderItemResponse item = response.items().get(0);
        assertThat(item.productId()).isEqualTo(product.getId());
        assertThat(item.productName()).isEqualTo(product.getName());
        assertThat(item.subtotal().amount()).isEqualByComparingTo("238.00");
        assertThat(response.allocations()).hasSize(1);
        assertThat(response.allocations().get(0).warehouseId()).isEqualTo(warehouse.getId());
        assertThat(response.deliveryAddress().city()).isEqualTo("Bogota");
        assertThat(response.invoiceId()).isEqualTo(order.getInvoice().getId());
        assertThat(response.createdAt()).isEqualTo(order.getCreatedAt());
    }

    @Test
    void invoiceMapperMapsMoneyBreakdown() {
        Seller seller = DomainFixtures.seller(DomainFixtures.admin());
        Product product = DomainFixtures.publishedPhysicalProduct(seller, DomainFixtures.operator());
        Order order = deliveredOrder(product, seller.getWarehouses().get(0).getId());

        InvoiceResponse response = invoiceMapper.toResponse(order.getInvoice());

        assertThat(response.id()).isEqualTo(order.getInvoice().getId());
        assertThat(response.orderId()).isEqualTo(order.getId());
        assertThat(response.number()).isEqualTo("INV-001");
        assertThat(response.subtotal().currency()).isEqualTo("COP");
        assertThat(response.taxes().currency()).isEqualTo("COP");
        assertThat(response.total().amount()).isEqualByComparingTo("238.00");
        assertThat(response.status()).isEqualTo("ISSUED");
        assertThat(response.issuedAt()).isEqualTo(order.getInvoice().getIssuedAt());
    }

    @Test
    void shipmentMapperMapsOrderWarehouseAndDestination() {
        Seller seller = DomainFixtures.seller(DomainFixtures.admin());
        Product product = DomainFixtures.publishedPhysicalProduct(seller, DomainFixtures.operator());
        Warehouse warehouse = seller.getWarehouses().get(0);
        Order order = deliveredOrder(product, warehouse.getId());
        Shipment shipment = Shipment.create(order, warehouse);
        shipment.startShipping("TRK-001");

        ShipmentResponse response = shipmentMapper.toResponse(shipment);

        assertThat(response.id()).isEqualTo(shipment.getId());
        assertThat(response.orderId()).isEqualTo(order.getId());
        assertThat(response.originWarehouseId()).isEqualTo(warehouse.getId());
        assertThat(response.destination().city()).isEqualTo("Bogota");
        assertThat(response.trackingNumber()).isEqualTo("TRK-001");
        assertThat(response.status()).isEqualTo("IN_TRANSIT");
    }

    @Test
    void returnAndRefundMappersMapReferencesAndMoney() {
        Seller seller = DomainFixtures.seller(DomainFixtures.admin());
        Product product = DomainFixtures.publishedPhysicalProduct(seller, DomainFixtures.operator());
        Order order = deliveredOrder(product, seller.getWarehouses().get(0).getId());
        Return returnRequest = Return.request(order, "Broken item", REQUESTED_AT);

        ReturnResponse returnResponse = returnMapper.toResponse(returnRequest);
        assertThat(returnResponse.id()).isEqualTo(returnRequest.getId());
        assertThat(returnResponse.orderId()).isEqualTo(order.getId());
        assertThat(returnResponse.reason()).isEqualTo("Broken item");
        assertThat(returnResponse.status()).isEqualTo("REQUESTED");
        assertThat(returnResponse.requestedAt()).isEqualTo(REQUESTED_AT);

        returnRequest.approve();
        Refund refund = Refund.create(returnRequest, Money.cop(new BigDecimal("50.00")),
                order.getTotalAmount());

        RefundResponse refundResponse = refundMapper.toResponse(refund);
        assertThat(refundResponse.id()).isEqualTo(refund.getId());
        assertThat(refundResponse.returnId()).isEqualTo(returnRequest.getId());
        assertThat(refundResponse.amount().amount()).isEqualByComparingTo("50.00");
        assertThat(refundResponse.amount().currency()).isEqualTo("COP");
        assertThat(refundResponse.status()).isEqualTo("PENDING");
    }

    @Test
    void reportMapperMapsAllReports() {
        SalesReport salesReport = new SalesReport(
                LocalDate.parse("2026-01-01"),
                LocalDate.parse("2026-01-31"),
                3,
                Money.cop(new BigDecimal("1500.00")));
        SalesReportResponse salesResponse = reportMapper.toResponse(salesReport);
        assertThat(salesResponse.from()).isEqualTo(LocalDate.parse("2026-01-01"));
        assertThat(salesResponse.to()).isEqualTo(LocalDate.parse("2026-01-31"));
        assertThat(salesResponse.orderCount()).isEqualTo(3);
        assertThat(salesResponse.grossRevenue().amount()).isEqualByComparingTo("1500.00");
        assertThat(salesResponse.grossRevenue().currency()).isEqualTo("COP");
        assertThat(salesResponse.currency()).isEqualTo("COP");
        assertThat(salesResponse.generatedAt()).isNotNull();

        OrderSummary orderSummary = new OrderSummary(10, 2, 3, 1, 2, 1, 1);
        OrderSummaryResponse orderResponse = reportMapper.toResponse(orderSummary);
        assertThat(orderResponse.totalOrders()).isEqualTo(10);
        assertThat(orderResponse.ordersByStatus())
                .containsEntry("PENDING_PAYMENT", 2)
                .containsEntry("PAID", 3)
                .containsEntry("DISPATCHED", 1)
                .containsEntry("DELIVERED", 2)
                .containsEntry("FINALIZED", 1)
                .containsEntry("CANCELLED", 1);
        assertThat(orderResponse.paid()).isEqualTo(3);

        InventorySummary inventorySummary = new InventorySummary(5, 40, 10, 2);
        InventorySummaryResponse inventoryResponse = reportMapper.toResponse(inventorySummary);
        assertThat(inventoryResponse.totalRecords()).isEqualTo(5);
        assertThat(inventoryResponse.totalAvailable()).isEqualTo(40);
        assertThat(inventoryResponse.totalReserved()).isEqualTo(10);
        assertThat(inventoryResponse.damagedRecords()).isEqualTo(2);
        assertThat(inventoryResponse.byWarehouse()).isEmpty();

        ReturnSummary returnSummary = new ReturnSummary(6, 1, 2, 1, 2);
        ReturnSummaryResponse returnResponse = reportMapper.toResponse(returnSummary);
        assertThat(returnResponse.totalReturns()).isEqualTo(6);
        assertThat(returnResponse.returnsByStatus())
                .containsEntry("REQUESTED", 1)
                .containsEntry("APPROVED", 2)
                .containsEntry("REJECTED", 1)
                .containsEntry("COMPLETED", 2);
        assertThat(returnResponse.approved()).isEqualTo(2);

        UserSummary userSummary = new UserSummary(10, Map.of(UserRole.BUYER, 4, UserRole.SELLER, 6));
        UserSummaryResponse userResponse = reportMapper.toResponse(userSummary);
        assertThat(userResponse.totalUsers()).isEqualTo(10);
        assertThat(userResponse.usersByRole())
                .containsEntry("BUYER", 4)
                .containsEntry("SELLER", 6)
                .containsEntry("ADMIN", 0);
        assertThat(userResponse.active()).isZero();
        assertThat(userResponse.blocked()).isZero();
        assertThat(userResponse.inactive()).isZero();
    }

    @Test
    void auditLogMapperMapsCatalogCodes() {
        User actor = DomainFixtures.admin();
        AuditLog auditLog = AuditLog.create(
                AuditAction.USER_CREATED,
                AuditEntityType.USER,
                actor.getId(),
                actor,
                REQUESTED_AT,
                "created");

        AuditLogResponse response = auditLogMapper.toResponse(auditLog);

        assertThat(response.id()).isEqualTo(auditLog.getId());
        assertThat(response.action()).isEqualTo("USER_CREATED");
        assertThat(response.entityType()).isEqualTo("USER");
        assertThat(response.entityId()).isEqualTo(actor.getId());
        assertThat(response.performedBy()).isEqualTo(actor.getId());
        assertThat(response.role()).isEqualTo("ADMIN");
        assertThat(response.occurredAt()).isEqualTo(REQUESTED_AT);
        assertThat(response.details()).isEqualTo("created");
    }

    @Test
    void helpersBuildDomainObjectsWithExpectedMoneyScale() {
        Money money = buyerMapper.toMoney(new BigDecimal("10.5"));
        assertThat(money.getAmount()).isEqualByComparingTo("10.50");
        assertThat(money.getAmount().scale()).isEqualTo(2);
        assertThat(money.getCurrency().getCurrencyCode()).isEqualTo("COP");

        Address address = buyerMapper.toAddress(
                new AddressRequest("Calle 1", "Bogota", "Cundinamarca", null, "Colombia"));
        AddressResponse response = new AddressResponse(
                address.getStreet(), address.getCity(), address.getRegion(),
                address.getPostalCode(), address.getCountry());
        assertThat(response.street()).isEqualTo("Calle 1");
        assertThat(response.city()).isEqualTo("Bogota");
        assertThat(response.postalCode()).isNull();
        assertThat(response.country()).isEqualTo("Colombia");
    }

    private static Order deliveredOrder(Product product, String warehouseId) {
        Buyer buyer = DomainFixtures.buyer();
        Cart cart = DomainFixtures.cartWith(buyer, product, 2);
        Order order = Order.fromCart(cart, DomainFixtures.address(), REQUESTED_AT);
        order.assignAllocations(List.of(StockAllocation.of(product.getId(), warehouseId, 2)));
        order.confirmPayment(Instant.parse("2026-01-15T10:05:00Z"));
        Invoice invoice = Invoice.create(order, "INV-001", new BigDecimal("0.19"),
                Instant.parse("2026-01-15T10:06:00Z"));
        order.attachInvoice(invoice);
        order.markDispatched();
        order.markDelivered();
        return order;
    }
}
