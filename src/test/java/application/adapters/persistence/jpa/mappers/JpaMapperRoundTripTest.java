package application.adapters.persistence.jpa.mappers;

import static org.assertj.core.api.Assertions.assertThat;

import application.adapters.persistence.jpa.entities.BuyerJpaEntity;
import application.adapters.persistence.jpa.entities.CartJpaEntity;
import application.adapters.persistence.jpa.entities.InventoryJpaEntity;
import application.adapters.persistence.jpa.entities.InventoryMovementJpaEntity;
import application.adapters.persistence.jpa.entities.InvoiceJpaEntity;
import application.adapters.persistence.jpa.entities.OrderJpaEntity;
import application.adapters.persistence.jpa.entities.ProductJpaEntity;
import application.adapters.persistence.jpa.entities.RefundJpaEntity;
import application.adapters.persistence.jpa.entities.ReturnJpaEntity;
import application.adapters.persistence.jpa.entities.SellerJpaEntity;
import application.adapters.persistence.jpa.entities.ShipmentJpaEntity;
import application.adapters.persistence.jpa.entities.UserJpaEntity;
import application.adapters.persistence.jpa.entities.WarehouseJpaEntity;
import application.domain.enums.CommercialStatus;
import application.domain.enums.InventoryCondition;
import application.domain.enums.MovementType;
import application.domain.enums.OrderStatus;
import application.domain.enums.PaymentStatus;
import application.domain.enums.ProductStatus;
import application.domain.enums.ProductType;
import application.domain.enums.UserRole;
import application.domain.enums.UserStatus;
import application.domain.enums.VariantType;
import application.domain.enums.WarehouseType;
import application.domain.models.Buyer;
import application.domain.models.Cart;
import application.domain.models.CartItem;
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
import application.domain.valueobjects.Address;
import application.domain.valueobjects.Email;
import application.domain.valueobjects.FullName;
import application.domain.valueobjects.IdentityDocument;
import application.domain.valueobjects.Money;
import application.domain.valueobjects.PasswordHash;
import application.domain.valueobjects.ProductVariant;
import application.domain.valueobjects.StockAllocation;
import application.domain.valueobjects.TaxId;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class JpaMapperRoundTripTest {

    private static final Instant NOW = Instant.parse("2026-03-15T10:15:30Z");

    private final UserJpaMapper userMapper = new UserJpaMapper();
    private final BuyerJpaMapper buyerMapper = new BuyerJpaMapper();
    private final SellerJpaMapper sellerMapper = new SellerJpaMapper();
    private final WarehouseJpaMapper warehouseMapper = new WarehouseJpaMapper();
    private final ProductJpaMapper productMapper = new ProductJpaMapper();
    private final InventoryJpaMapper inventoryMapper = new InventoryJpaMapper();
    private final InventoryMovementJpaMapper inventoryMovementMapper = new InventoryMovementJpaMapper();
    private final CartJpaMapper cartMapper = new CartJpaMapper();
    private final OrderJpaMapper orderMapper = new OrderJpaMapper();
    private final InvoiceJpaMapper invoiceMapper = new InvoiceJpaMapper();
    private final ShipmentJpaMapper shipmentMapper = new ShipmentJpaMapper();
    private final ReturnJpaMapper returnMapper = new ReturnJpaMapper();
    private final RefundJpaMapper refundMapper = new RefundJpaMapper();

    @Test
    void userMapperRoundTripPreservesEveryPersistedField() {
        User user = user("user-1", UserRole.ADMIN, UserStatus.BLOCKED, 7L);

        UserJpaEntity entity = userMapper.toEntity(user);
        User roundTrip = userMapper.toDomain(entity);

        assertThat(entity.getId()).isEqualTo(user.getId());
        assertThat(entity.getFullNameValue()).isEqualTo(user.getFullName().getValue());
        assertThat(entity.getEmailValue()).isEqualTo(user.getEmail().getValue());
        assertThat(entity.getIdentityDocumentValue()).isEqualTo(user.getIdentityDocument().getValue());
        assertThat(entity.getRoleCode()).isEqualTo(user.getRole());
        assertThat(entity.getStatusCode()).isEqualTo(user.getStatus());
        assertThat(entity.getPasswordHashValue()).isEqualTo(user.getPasswordHash().getValue());
        assertThat(entity.getAuthTokenVersion()).isEqualTo(user.getAuthTokenVersion());

        assertThat(roundTrip.getId()).isEqualTo(user.getId());
        assertThat(roundTrip.getFullName()).isEqualTo(user.getFullName());
        assertThat(roundTrip.getEmail()).isEqualTo(user.getEmail());
        assertThat(roundTrip.getIdentityDocument()).isEqualTo(user.getIdentityDocument());
        assertThat(roundTrip.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(roundTrip.getStatus()).isEqualTo(UserStatus.BLOCKED);
        assertThat(roundTrip.getPasswordHash()).isEqualTo(user.getPasswordHash());
        assertThat(roundTrip.getAuthTokenVersion()).isEqualTo(7L);
    }

    @Test
    void buyerMapperRoundTripPreservesUserAddressesAndCommercialStatus() {
        Buyer buyer = buyer("buyer-1");

        BuyerJpaEntity entity = buyerMapper.toEntity(buyer);
        Buyer roundTrip = buyerMapper.toDomain(entity, buyer.getUser());

        assertThat(entity.getId()).isEqualTo(buyer.getId());
        assertThat(entity.getUserId()).isEqualTo(buyer.getUser().getId());
        assertThat(entity.getMainAddress().getStreet()).isEqualTo("Main Street 1");
        assertThat(entity.getAdditionalAddresses()).hasSize(1);

        assertThat(roundTrip.getId()).isEqualTo(buyer.getId());
        assertThat(roundTrip.getUser().getId()).isEqualTo(buyer.getUser().getId());
        assertThat(roundTrip.getMainAddress()).isEqualTo(buyer.getMainAddress());
        assertThat(roundTrip.getAdditionalAddresses())
                .containsExactlyElementsOf(buyer.getAdditionalAddresses());
        assertThat(roundTrip.getCommercialStatus()).isEqualTo(CommercialStatus.RESTRICTED);
    }

    @Test
    void sellerMapperRoundTripPreservesUserTaxIdAndWarehouses() {
        Seller seller = seller("seller-1");
        Warehouse warehouse = sellerWarehouse("warehouse-seller-1", seller);

        SellerJpaEntity entity = sellerMapper.toEntity(seller);
        Seller roundTrip = sellerMapper.toDomain(entity, seller.getUser(), List.of(warehouse));

        assertThat(entity.getId()).isEqualTo(seller.getId());
        assertThat(entity.getUserId()).isEqualTo(seller.getUser().getId());
        assertThat(entity.getTaxIdValue()).isEqualTo(seller.getTaxId().getValue());

        assertThat(roundTrip.getId()).isEqualTo(seller.getId());
        assertThat(roundTrip.getUser().getId()).isEqualTo(seller.getUser().getId());
        assertThat(roundTrip.getTaxId()).isEqualTo(seller.getTaxId());
        assertThat(roundTrip.getWarehouses()).extracting(Warehouse::getId)
                .containsExactly(warehouse.getId());
    }

    @Test
    void warehouseMapperRoundTripPreservesAddressTypeAndNullableOwner() {
        Seller seller = seller("seller-warehouse");
        User responsible = user("responsible-1", UserRole.ADMIN, UserStatus.ACTIVE, 0L);
        Warehouse sellerWarehouse = Warehouse.reconstitute("warehouse-seller", "Seller warehouse",
                address("Seller Street"), WarehouseType.SELLER, seller, responsible);
        Warehouse marketplace = Warehouse.reconstitute("warehouse-market", "Marketplace warehouse",
                address("Market Street"), WarehouseType.MARKETPLACE, null, responsible);

        WarehouseJpaEntity sellerEntity = warehouseMapper.toEntity(sellerWarehouse);
        Warehouse sellerRoundTrip = warehouseMapper.toDomain(sellerEntity, seller, responsible);
        assertThat(sellerEntity.getOwnerSellerId()).isEqualTo(seller.getId());
        assertThat(sellerRoundTrip.getId()).isEqualTo(sellerWarehouse.getId());
        assertThat(sellerRoundTrip.getName()).isEqualTo("Seller warehouse");
        assertThat(sellerRoundTrip.getLocation()).isEqualTo(sellerWarehouse.getLocation());
        assertThat(sellerRoundTrip.getType()).isEqualTo(WarehouseType.SELLER);
        assertThat(sellerRoundTrip.getOwner().getId()).isEqualTo(seller.getId());
        assertThat(sellerRoundTrip.getResponsible().getId()).isEqualTo(responsible.getId());

        WarehouseJpaEntity marketEntity = warehouseMapper.toEntity(marketplace);
        Warehouse marketRoundTrip = warehouseMapper.toDomain(marketEntity, null, responsible);
        assertThat(marketEntity.getOwnerSellerId()).isNull();
        assertThat(marketRoundTrip.getId()).isEqualTo(marketplace.getId());
        assertThat(marketRoundTrip.getName()).isEqualTo("Marketplace warehouse");
        assertThat(marketRoundTrip.getLocation()).isEqualTo(marketplace.getLocation());
        assertThat(marketRoundTrip.getType()).isEqualTo(WarehouseType.MARKETPLACE);
        assertThat(marketRoundTrip.getOwner()).isNull();
        assertThat(marketRoundTrip.getResponsible().getId()).isEqualTo(responsible.getId());
    }

    @Test
    void productMapperRoundTripPreservesTextCatalogMoneyAndVariants() {
        Seller seller = seller("seller-product");
        List<ProductVariant> variants = List.of(
                ProductVariant.of(VariantType.COLOR, "Black"),
                ProductVariant.of(VariantType.SIZE, "XL"));
        Product product = Product.reconstitute("product-1", seller, "Laptop", "Rich laptop",
                ProductType.PHYSICAL, ProductStatus.PUBLISHED,
                Money.cop(new BigDecimal("1234.50")), variants);

        ProductJpaEntity entity = productMapper.toEntity(product);
        List<ProductVariant> roundTripVariants = variants.stream()
                .map(variant -> productMapper.toDomain(
                        productMapper.toEntity(variant, product.getId())))
                .toList();
        Product roundTrip = productMapper.toDomain(entity, seller, roundTripVariants);

        assertThat(entity.getId()).isEqualTo(product.getId());
        assertThat(entity.getSellerId()).isEqualTo(seller.getId());
        assertThat(entity.getName()).isEqualTo("Laptop");
        assertThat(entity.getDescription()).isEqualTo("Rich laptop");
        assertThat(entity.getTypeCode()).isEqualTo(ProductType.PHYSICAL);
        assertThat(entity.getStatusCode()).isEqualTo(ProductStatus.PUBLISHED);
        assertThat(entity.getPriceAmount()).isEqualByComparingTo("1234.50");
        assertThat(entity.getPriceCurrency()).isEqualTo("COP");

        assertThat(roundTrip.getId()).isEqualTo(product.getId());
        assertThat(roundTrip.getSeller().getId()).isEqualTo(seller.getId());
        assertThat(roundTrip.getName()).isEqualTo("Laptop");
        assertThat(roundTrip.getDescription()).isEqualTo("Rich laptop");
        assertThat(roundTrip.getType()).isEqualTo(ProductType.PHYSICAL);
        assertThat(roundTrip.getStatus()).isEqualTo(ProductStatus.PUBLISHED);
        assertThat(roundTrip.getPrice().getAmount()).isEqualByComparingTo("1234.50");
        assertThat(roundTrip.getPrice().getAmount().scale()).isEqualTo(2);
        assertThat(roundTrip.getPrice().getCurrency().getCurrencyCode()).isEqualTo("COP");
        assertThat(roundTrip.getVariants()).containsExactlyElementsOf(variants);
    }

    @Test
    void inventoryMapperRoundTripPreservesQuantitiesAndCondition() {
        Seller seller = seller("seller-inventory");
        Warehouse warehouse = sellerWarehouse("warehouse-inventory", seller);
        Product product = physicalProduct("product-inventory", seller, "100.00");
        Inventory inventory = Inventory.reconstitute("inventory-1", product, warehouse,
                25, 7, InventoryCondition.DAMAGED);

        InventoryJpaEntity entity = inventoryMapper.toEntity(inventory);
        Inventory roundTrip = inventoryMapper.toDomain(entity, product, warehouse);

        assertThat(entity.getId()).isEqualTo(inventory.getId());
        assertThat(entity.getProductId()).isEqualTo(product.getId());
        assertThat(entity.getWarehouseId()).isEqualTo(warehouse.getId());
        assertThat(entity.getAvailableQuantity()).isEqualTo(25);
        assertThat(entity.getReservedQuantity()).isEqualTo(7);
        assertThat(entity.getConditionCode()).isEqualTo(InventoryCondition.DAMAGED);

        assertThat(roundTrip.getId()).isEqualTo(inventory.getId());
        assertThat(roundTrip.getProduct().getId()).isEqualTo(product.getId());
        assertThat(roundTrip.getWarehouse().getId()).isEqualTo(warehouse.getId());
        assertThat(roundTrip.getAvailableQuantity()).isEqualTo(25);
        assertThat(roundTrip.getReservedQuantity()).isEqualTo(7);
        assertThat(roundTrip.getCondition()).isEqualTo(InventoryCondition.DAMAGED);
    }

    @Test
    void inventoryMovementMapperRoundTripPreservesTypeQuantityActorAndTimestamp() {
        Seller seller = seller("seller-movement");
        Warehouse warehouse = sellerWarehouse("warehouse-movement", seller);
        Product product = physicalProduct("product-movement", seller, "80.00");
        Inventory inventory = Inventory.reconstitute("inventory-movement", product, warehouse,
                10, 0, InventoryCondition.GOOD);
        User performer = user("performer-1", UserRole.LOGISTIC_OPERATOR, UserStatus.ACTIVE, 3L);
        InventoryMovement movement = InventoryMovement.reconstitute("movement-1", inventory,
                MovementType.ADJUSTMENT, -3, performer, NOW);

        InventoryMovementJpaEntity entity = inventoryMovementMapper.toEntity(movement);
        InventoryMovement roundTrip = inventoryMovementMapper.toDomain(entity, inventory, performer);

        assertThat(entity.getId()).isEqualTo(movement.getId());
        assertThat(entity.getInventoryId()).isEqualTo(inventory.getId());
        assertThat(entity.getTypeCode()).isEqualTo(MovementType.ADJUSTMENT);
        assertThat(entity.getQuantity()).isEqualTo(-3);
        assertThat(entity.getPerformedByUserId()).isEqualTo(performer.getId());
        assertThat(entity.getOccurredAt()).isEqualTo(NOW);

        assertThat(roundTrip.getId()).isEqualTo(movement.getId());
        assertThat(roundTrip.getInventory().getId()).isEqualTo(inventory.getId());
        assertThat(roundTrip.getType()).isEqualTo(MovementType.ADJUSTMENT);
        assertThat(roundTrip.getQuantity()).isEqualTo(-3);
        assertThat(roundTrip.getPerformedBy().getId()).isEqualTo(performer.getId());
        assertThat(roundTrip.getOccurredAt()).isEqualTo(NOW);
    }

    @Test
    void cartMapperRoundTripPreservesBuyerAndItems() {
        Seller seller = seller("seller-cart");
        Product first = physicalProduct("product-cart-1", seller, "100.00");
        Product second = physicalProduct("product-cart-2", seller, "50.00");
        Buyer buyer = buyer("buyer-cart");
        Cart cart = Cart.reconstitute("cart-1", buyer, List.of(
                CartItem.reconstitute(first, 2, Money.cop(new BigDecimal("99.99"))),
                CartItem.reconstitute(second, 1, Money.cop(new BigDecimal("12.34")))));

        CartJpaEntity entity = cartMapper.toEntity(cart);
        List<CartItem> roundTripItems = cart.getItems().stream()
                .map(item -> cartMapper.toDomain(
                        cartMapper.toEntity(item, cart.getId()), item.getProduct()))
                .toList();
        Cart roundTrip = cartMapper.toDomain(entity, buyer, roundTripItems);

        assertThat(entity.getId()).isEqualTo(cart.getId());
        assertThat(entity.getBuyerId()).isEqualTo(buyer.getId());
        assertThat(roundTrip.getId()).isEqualTo(cart.getId());
        assertThat(roundTrip.getBuyer().getId()).isEqualTo(buyer.getId());
        assertThat(roundTrip.getItems()).hasSize(2);
        assertThat(roundTrip.getItems()).extracting(item -> item.getProduct().getId())
                .containsExactly(first.getId(), second.getId());
        assertThat(roundTrip.getItems()).extracting(CartItem::getQuantity)
                .containsExactly(2, 1);
        assertThat(roundTrip.getItems().get(0).getUnitPrice())
                .isEqualTo(Money.cop(new BigDecimal("99.99")));
        assertThat(roundTrip.getItems().get(1).getUnitPrice())
                .isEqualTo(Money.cop(new BigDecimal("12.34")));
    }

    @Test
    void orderMapperRoundTripPreservesBuyerItemsAllocationsAddressAndNullableInvoice() {
        Seller seller = seller("seller-order");
        Product first = physicalProduct("product-order-1", seller, "100.00");
        Product second = physicalProduct("product-order-2", seller, "50.00");
        Buyer buyer = buyer("buyer-order");
        List<CartItem> items = List.of(
                CartItem.reconstitute(first, 3, Money.cop(new BigDecimal("100.00"))),
                CartItem.reconstitute(second, 1, Money.cop(new BigDecimal("50.00"))));
        List<StockAllocation> allocations = List.of(
                StockAllocation.of(first.getId(), "warehouse-1", 3),
                StockAllocation.of(second.getId(), "warehouse-2", 1));
        Order order = order("order-1", buyer, items, Money.cop(new BigDecimal("350.00")),
                OrderStatus.PAID, PaymentStatus.CONFIRMED, allocations, null, NOW);

        OrderJpaEntity entity = orderMapper.toEntity(order);
        List<CartItem> roundTripItems = order.getItems().stream()
                .map(item -> orderMapper.toDomainOrderItem(
                        orderMapper.toEntity(item, order.getId()), item.getProduct()))
                .toList();
        List<StockAllocation> roundTripAllocations = order.getAllocations().stream()
                .map(allocation -> orderMapper.toDomain(
                        orderMapper.toEntity(allocation, order.getId())))
                .toList();
        Order roundTrip = orderMapper.toDomain(entity, buyer, roundTripItems,
                roundTripAllocations, null);

        assertThat(entity.getId()).isEqualTo(order.getId());
        assertThat(entity.getBuyerId()).isEqualTo(buyer.getId());
        assertThat(entity.getStatusCode()).isEqualTo(OrderStatus.PAID);
        assertThat(entity.getTotalAmount()).isEqualByComparingTo("350.00");
        assertThat(entity.getTotalCurrency()).isEqualTo("COP");
        assertThat(entity.getPaymentStatusCode()).isEqualTo(PaymentStatus.CONFIRMED);
        assertThat(entity.getInvoiceId()).isNull();
        assertThat(entity.getCreatedAt()).isEqualTo(NOW);

        assertThat(roundTrip.getId()).isEqualTo(order.getId());
        assertThat(roundTrip.getBuyer().getId()).isEqualTo(buyer.getId());
        assertThat(roundTrip.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(roundTrip.getTotalAmount().getAmount()).isEqualByComparingTo("350.00");
        assertThat(roundTrip.getTotalAmount().getAmount().scale()).isEqualTo(2);
        assertThat(roundTrip.getPaymentStatus()).isEqualTo(PaymentStatus.CONFIRMED);
        assertThat(roundTrip.getDeliveryAddress().getStreet()).isEqualTo("Delivery Street");
        assertThat(roundTrip.getInvoice()).isNull();
        assertThat(roundTrip.getCreatedAt()).isEqualTo(NOW);
        assertThat(roundTrip.getItems()).extracting(item -> item.getProduct().getId())
                .containsExactly(first.getId(), second.getId());
        assertThat(roundTrip.getAllocations()).containsExactlyElementsOf(allocations);
    }

    @Test
    void invoiceMapperRoundTripPreservesOrderNumberMoneyStatusAndTimestamp() {
        Seller seller = seller("seller-invoice");
        Product product = physicalProduct("product-invoice", seller, "250.00");
        Buyer buyer = buyer("buyer-invoice");
        Order order = order("order-invoice", buyer,
                List.of(CartItem.reconstitute(product, 1, Money.cop(new BigDecimal("250.00")))),
                Money.cop(new BigDecimal("250.00")), OrderStatus.PAID,
                PaymentStatus.CONFIRMED, List.of(), null, NOW);
        Invoice invoice = Invoice.create(order, "INV-001", new BigDecimal("0.19"),
                NOW.plusSeconds(60));
        order.attachInvoice(invoice);

        InvoiceJpaEntity entity = invoiceMapper.toEntity(invoice);
        Invoice roundTrip = invoiceMapper.toDomain(entity, order);

        assertThat(entity.getId()).isEqualTo(invoice.getId());
        assertThat(entity.getOrderId()).isEqualTo(order.getId());
        assertThat(entity.getNumber()).isEqualTo("INV-001");
        assertThat(entity.getSubtotalAmount()).isEqualByComparingTo(
                invoice.getSubtotal().getAmount());
        assertThat(entity.getTaxesAmount()).isEqualByComparingTo(invoice.getTaxes().getAmount());
        assertThat(entity.getTotalAmount()).isEqualByComparingTo(invoice.getTotal().getAmount());
        assertThat(entity.getStatusCode()).isEqualTo(invoice.getStatus());
        assertThat(entity.getIssuedAt()).isEqualTo(invoice.getIssuedAt());

        assertThat(roundTrip.getId()).isEqualTo(invoice.getId());
        assertThat(roundTrip.getOrder().getId()).isEqualTo(order.getId());
        assertThat(roundTrip.getNumber()).isEqualTo("INV-001");
        assertThat(roundTrip.getSubtotal()).isEqualTo(invoice.getSubtotal());
        assertThat(roundTrip.getTaxes()).isEqualTo(invoice.getTaxes());
        assertThat(roundTrip.getTotal()).isEqualTo(invoice.getTotal());
        assertThat(roundTrip.getSubtotal().getAmount().scale()).isEqualTo(2);
        assertThat(roundTrip.getTaxes().getAmount().scale()).isEqualTo(2);
        assertThat(roundTrip.getTotal().getAmount().scale()).isEqualTo(2);
        assertThat(roundTrip.getStatus()).isEqualTo(invoice.getStatus());
        assertThat(roundTrip.getIssuedAt()).isEqualTo(invoice.getIssuedAt());
    }

    @Test
    void shipmentMapperRoundTripPreservesOrderOriginDestinationTrackingAndStatus() {
        Seller seller = seller("seller-shipment");
        Warehouse origin = sellerWarehouse("warehouse-shipment", seller);
        Product product = physicalProduct("product-shipment", seller, "75.00");
        Buyer buyer = buyer("buyer-shipment");
        Order order = order("order-shipment", buyer,
                List.of(CartItem.reconstitute(product, 2, Money.cop(new BigDecimal("75.00")))),
                Money.cop(new BigDecimal("150.00")), OrderStatus.PAID,
                PaymentStatus.CONFIRMED, List.of(), null, NOW);

        Shipment pending = Shipment.create(order, origin);
        ShipmentJpaEntity pendingEntity = shipmentMapper.toEntity(pending);
        Shipment pendingRoundTrip = shipmentMapper.toDomain(pendingEntity, order, origin);
        assertThat(pendingEntity.getId()).isEqualTo(pending.getId());
        assertThat(pendingEntity.getOrderId()).isEqualTo(order.getId());
        assertThat(pendingEntity.getOriginWarehouseId()).isEqualTo(origin.getId());
        assertThat(pendingEntity.getTrackingNumber()).isNull();
        assertThat(pendingRoundTrip.getId()).isEqualTo(pending.getId());
        assertThat(pendingRoundTrip.getOrder().getId()).isEqualTo(order.getId());
        assertThat(pendingRoundTrip.getOriginWarehouse().getId()).isEqualTo(origin.getId());
        assertThat(pendingRoundTrip.getDestination()).isEqualTo(order.getDeliveryAddress());
        assertThat(pendingRoundTrip.getTrackingNumber()).isNull();
        assertThat(pendingRoundTrip.getStatus()).isEqualTo(pending.getStatus());

        Shipment started = Shipment.create(order, origin);
        started.startShipping("TRK-123");
        ShipmentJpaEntity startedEntity = shipmentMapper.toEntity(started);
        Shipment startedRoundTrip = shipmentMapper.toDomain(startedEntity, order, origin);
        assertThat(startedRoundTrip.getTrackingNumber()).isEqualTo("TRK-123");
        assertThat(startedRoundTrip.getStatus()).isEqualTo(started.getStatus());
        assertThat(startedRoundTrip.getDestination()).isEqualTo(started.getDestination());
    }

    @Test
    void returnMapperRoundTripPreservesOrderReasonStatusAndTimestamp() {
        Seller seller = seller("seller-return");
        Product product = physicalProduct("product-return", seller, "60.00");
        Buyer buyer = buyer("buyer-return");
        Order order = order("order-return", buyer,
                List.of(CartItem.reconstitute(product, 1, Money.cop(new BigDecimal("60.00")))),
                Money.cop(new BigDecimal("60.00")), OrderStatus.DELIVERED,
                PaymentStatus.CONFIRMED, List.of(), null, NOW);
        Return returnRequest = Return.request(order, "Damaged on arrival", NOW);
        returnRequest.approve();

        ReturnJpaEntity entity = returnMapper.toEntity(returnRequest);
        Return roundTrip = returnMapper.toDomain(entity, order);

        assertThat(entity.getId()).isEqualTo(returnRequest.getId());
        assertThat(entity.getOrderId()).isEqualTo(order.getId());
        assertThat(entity.getReason()).isEqualTo("Damaged on arrival");
        assertThat(entity.getStatusCode()).isEqualTo(returnRequest.getStatus());
        assertThat(entity.getRequestedAt()).isEqualTo(NOW);

        assertThat(roundTrip.getId()).isEqualTo(returnRequest.getId());
        assertThat(roundTrip.getOrder().getId()).isEqualTo(order.getId());
        assertThat(roundTrip.getReason()).isEqualTo("Damaged on arrival");
        assertThat(roundTrip.getStatus()).isEqualTo(returnRequest.getStatus());
        assertThat(roundTrip.getRequestedAt()).isEqualTo(NOW);
    }

    @Test
    void refundMapperRoundTripPreservesReturnAmountAndStatus() {
        Seller seller = seller("seller-refund");
        Product product = physicalProduct("product-refund", seller, "90.00");
        Buyer buyer = buyer("buyer-refund");
        Order order = order("order-refund", buyer,
                List.of(CartItem.reconstitute(product, 1, Money.cop(new BigDecimal("90.00")))),
                Money.cop(new BigDecimal("90.00")), OrderStatus.DELIVERED,
                PaymentStatus.CONFIRMED, List.of(), null, NOW);
        Return returnRequest = Return.request(order, "Broken item", NOW);
        returnRequest.approve();
        Refund refund = Refund.create(returnRequest, Money.cop(new BigDecimal("40.00")),
                order.getTotalAmount());
        refund.process();

        RefundJpaEntity entity = refundMapper.toEntity(refund);
        Refund roundTrip = refundMapper.toDomain(entity, returnRequest);

        assertThat(entity.getId()).isEqualTo(refund.getId());
        assertThat(entity.getReturnId()).isEqualTo(returnRequest.getId());
        assertThat(entity.getAmountAmount()).isEqualByComparingTo("40.00");
        assertThat(entity.getAmountCurrency()).isEqualTo("COP");
        assertThat(entity.getStatusCode()).isEqualTo(refund.getStatus());

        assertThat(roundTrip.getId()).isEqualTo(refund.getId());
        assertThat(roundTrip.getReturnRequest().getId()).isEqualTo(returnRequest.getId());
        assertThat(roundTrip.getAmount().getAmount()).isEqualByComparingTo("40.00");
        assertThat(roundTrip.getAmount().getAmount().scale()).isEqualTo(2);
        assertThat(roundTrip.getAmount().getCurrency().getCurrencyCode()).isEqualTo("COP");
        assertThat(roundTrip.getStatus()).isEqualTo(refund.getStatus());
    }

    private static Address address(String street) {
        return Address.of(street, "Bogota", "Cundinamarca", "110111", "Colombia");
    }

    private static String document(String id) {
        return "DOC" + String.format("%08X", id.hashCode());
    }

    private static User user(String id, UserRole role, UserStatus status, long tokenVersion) {
        return User.reconstitute(id, FullName.of("User " + id), Email.of(id + "@nexus.test"),
                IdentityDocument.of(document(id)), role, status,
                PasswordHash.of("hash-" + id), tokenVersion);
    }

    private static Buyer buyer(String id) {
        return Buyer.reconstitute(id,
                user(id + "-user", UserRole.BUYER, UserStatus.ACTIVE, 2L),
                address("Main Street 1"),
                List.of(address("Second Street 2")),
                CommercialStatus.RESTRICTED);
    }

    private static Seller seller(String id) {
        return Seller.reconstitute(id,
                user(id + "-user", UserRole.SELLER, UserStatus.ACTIVE, 1L),
                TaxId.of("900123456-7"),
                List.of());
    }

    private static Warehouse sellerWarehouse(String id, Seller seller) {
        return Warehouse.reconstitute(id, "Warehouse " + id, address("Warehouse Street " + id),
                WarehouseType.SELLER, seller,
                user(id + "-responsible", UserRole.ADMIN, UserStatus.ACTIVE, 0L));
    }

    private static Product physicalProduct(String id, Seller seller, String price) {
        return Product.reconstitute(id, seller, "Product " + id, "Description " + id,
                ProductType.PHYSICAL, ProductStatus.PUBLISHED,
                Money.cop(new BigDecimal(price)), List.of());
    }

    private static Order order(String id,
                               Buyer buyer,
                               List<CartItem> items,
                               Money total,
                               OrderStatus status,
                               PaymentStatus paymentStatus,
                               List<StockAllocation> allocations,
                               Invoice invoice,
                               Instant createdAt) {
        return Order.reconstitute(id, buyer, items, status, total, paymentStatus, allocations,
                address("Delivery Street"), invoice, createdAt);
    }
}
