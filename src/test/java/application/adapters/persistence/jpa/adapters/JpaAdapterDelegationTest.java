package application.adapters.persistence.jpa.adapters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import application.adapters.persistence.jpa.entities.BuyerJpaEntity;
import application.adapters.persistence.jpa.entities.CartItemJpaEntity;
import application.adapters.persistence.jpa.entities.CartJpaEntity;
import application.adapters.persistence.jpa.entities.InventoryJpaEntity;
import application.adapters.persistence.jpa.entities.InventoryMovementJpaEntity;
import application.adapters.persistence.jpa.entities.InvoiceJpaEntity;
import application.adapters.persistence.jpa.entities.OrderAllocationJpaEntity;
import application.adapters.persistence.jpa.entities.OrderItemJpaEntity;
import application.adapters.persistence.jpa.entities.OrderJpaEntity;
import application.adapters.persistence.jpa.entities.ProductJpaEntity;
import application.adapters.persistence.jpa.entities.ProductVariantJpaEntity;
import application.adapters.persistence.jpa.entities.RefundJpaEntity;
import application.adapters.persistence.jpa.entities.ReturnJpaEntity;
import application.adapters.persistence.jpa.entities.SellerJpaEntity;
import application.adapters.persistence.jpa.entities.ShipmentJpaEntity;
import application.adapters.persistence.jpa.entities.UserJpaEntity;
import application.adapters.persistence.jpa.entities.WarehouseJpaEntity;
import application.adapters.persistence.jpa.mappers.BuyerJpaMapper;
import application.adapters.persistence.jpa.mappers.CartJpaMapper;
import application.adapters.persistence.jpa.mappers.InventoryJpaMapper;
import application.adapters.persistence.jpa.mappers.InventoryMovementJpaMapper;
import application.adapters.persistence.jpa.mappers.InvoiceJpaMapper;
import application.adapters.persistence.jpa.mappers.OrderJpaMapper;
import application.adapters.persistence.jpa.mappers.ProductJpaMapper;
import application.adapters.persistence.jpa.mappers.RefundJpaMapper;
import application.adapters.persistence.jpa.mappers.ReturnJpaMapper;
import application.adapters.persistence.jpa.mappers.SellerJpaMapper;
import application.adapters.persistence.jpa.mappers.ShipmentJpaMapper;
import application.adapters.persistence.jpa.mappers.UserJpaMapper;
import application.adapters.persistence.jpa.mappers.WarehouseJpaMapper;
import application.adapters.persistence.jpa.repositories.BuyerJpaRepository;
import application.adapters.persistence.jpa.repositories.CartItemJpaRepository;
import application.adapters.persistence.jpa.repositories.CartJpaRepository;
import application.adapters.persistence.jpa.repositories.InventoryJpaRepository;
import application.adapters.persistence.jpa.repositories.InventoryMovementJpaRepository;
import application.adapters.persistence.jpa.repositories.InvoiceJpaRepository;
import application.adapters.persistence.jpa.repositories.OrderAllocationJpaRepository;
import application.adapters.persistence.jpa.repositories.OrderItemJpaRepository;
import application.adapters.persistence.jpa.repositories.OrderJpaRepository;
import application.adapters.persistence.jpa.repositories.ProductJpaRepository;
import application.adapters.persistence.jpa.repositories.ProductVariantJpaRepository;
import application.adapters.persistence.jpa.repositories.RefundJpaRepository;
import application.adapters.persistence.jpa.repositories.ReturnJpaRepository;
import application.adapters.persistence.jpa.repositories.SellerJpaRepository;
import application.adapters.persistence.jpa.repositories.ShipmentJpaRepository;
import application.adapters.persistence.jpa.repositories.UserJpaRepository;
import application.adapters.persistence.jpa.repositories.WarehouseJpaRepository;
import application.domain.enums.CommercialStatus;
import application.domain.enums.InventoryCondition;
import application.domain.enums.InvoiceStatus;
import application.domain.enums.MovementType;
import application.domain.enums.OrderStatus;
import application.domain.enums.PaymentStatus;
import application.domain.enums.ProductStatus;
import application.domain.enums.ProductType;
import application.domain.enums.RefundStatus;
import application.domain.enums.ReturnStatus;
import application.domain.enums.ShipmentStatus;
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
class JpaAdapterDelegationTest {

    private static final Instant NOW = Instant.parse("2026-03-15T10:15:30Z");

    private final UserJpaMapper userMapper = new UserJpaMapper();
    private final BuyerJpaMapper buyerMapper = new BuyerJpaMapper();
    private final SellerJpaMapper sellerMapper = new SellerJpaMapper();
    private final WarehouseJpaMapper warehouseMapper = new WarehouseJpaMapper();
    private final ProductJpaMapper productMapper = new ProductJpaMapper();
    private final InventoryJpaMapper inventoryMapper = new InventoryJpaMapper();
    private final InventoryMovementJpaMapper movementMapper = new InventoryMovementJpaMapper();
    private final CartJpaMapper cartMapper = new CartJpaMapper();
    private final OrderJpaMapper orderMapper = new OrderJpaMapper();
    private final InvoiceJpaMapper invoiceMapper = new InvoiceJpaMapper();
    private final ShipmentJpaMapper shipmentMapper = new ShipmentJpaMapper();
    private final ReturnJpaMapper returnMapper = new ReturnJpaMapper();
    private final RefundJpaMapper refundMapper = new RefundJpaMapper();

    @Mock private UserJpaRepository userRepository;
    @Mock private BuyerJpaRepository buyerRepository;
    @Mock private SellerJpaRepository sellerRepository;
    @Mock private WarehouseJpaRepository warehouseRepository;
    @Mock private ProductJpaRepository productRepository;
    @Mock private ProductVariantJpaRepository variantRepository;
    @Mock private InventoryJpaRepository inventoryRepository;
    @Mock private InventoryMovementJpaRepository movementRepository;
    @Mock private CartJpaRepository cartRepository;
    @Mock private CartItemJpaRepository cartItemRepository;
    @Mock private OrderJpaRepository orderRepository;
    @Mock private OrderItemJpaRepository orderItemRepository;
    @Mock private OrderAllocationJpaRepository orderAllocationRepository;
    @Mock private InvoiceJpaRepository invoiceRepository;
    @Mock private ShipmentJpaRepository shipmentRepository;
    @Mock private ReturnJpaRepository returnRepository;
    @Mock private RefundJpaRepository refundRepository;
    @Mock private JpaDependencyResolver resolver;

    private UserJpaAdapter userAdapter;
    private BuyerJpaAdapter buyerAdapter;
    private SellerJpaAdapter sellerAdapter;
    private WarehouseJpaAdapter warehouseAdapter;
    private ProductJpaAdapter productAdapter;
    private InventoryJpaAdapter inventoryAdapter;
    private InventoryMovementJpaAdapter movementAdapter;
    private CartJpaAdapter cartAdapter;
    private OrderJpaAdapter orderAdapter;
    private InvoiceJpaAdapter invoiceAdapter;
    private ShipmentJpaAdapter shipmentAdapter;
    private ReturnJpaAdapter returnAdapter;
    private RefundJpaAdapter refundAdapter;

    @BeforeEach
    void setUp() {
        userAdapter = new UserJpaAdapter(userRepository, userMapper);
        buyerAdapter = new BuyerJpaAdapter(buyerRepository, buyerMapper, resolver);
        sellerAdapter = new SellerJpaAdapter(sellerRepository, sellerMapper, resolver);
        warehouseAdapter = new WarehouseJpaAdapter(warehouseRepository, warehouseMapper, resolver);
        productAdapter = new ProductJpaAdapter(
                productRepository, variantRepository, productMapper, resolver);
        inventoryAdapter = new InventoryJpaAdapter(inventoryRepository, inventoryMapper, resolver);
        movementAdapter = new InventoryMovementJpaAdapter(
                movementRepository, movementMapper, resolver);
        cartAdapter = new CartJpaAdapter(cartRepository, cartItemRepository, cartMapper, resolver);
        orderAdapter = new OrderJpaAdapter(orderRepository, orderItemRepository,
                orderAllocationRepository, orderMapper, resolver);
        invoiceAdapter = new InvoiceJpaAdapter(invoiceRepository, invoiceMapper, resolver);
        shipmentAdapter = new ShipmentJpaAdapter(shipmentRepository, shipmentMapper, resolver);
        returnAdapter = new ReturnJpaAdapter(returnRepository, returnMapper, resolver);
        refundAdapter = new RefundJpaAdapter(refundRepository, refundMapper, resolver);
    }

    @Test
    void saveUserBuildsEntityAndDelegatesToRepository() {
        User user = user("user-1", UserRole.ADMIN, UserStatus.ACTIVE);
        when(userRepository.save(any(UserJpaEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<UserJpaEntity> captor = ArgumentCaptor.forClass(UserJpaEntity.class);

        User result = userAdapter.save(user);

        verify(userRepository).save(captor.capture());
        UserJpaEntity entity = captor.getValue();
        assertThat(entity.getId()).isEqualTo("user-1");
        assertThat(entity.getEmailValue()).isEqualTo(user.getEmail().getValue());
        assertThat(entity.getRoleCode()).isEqualTo(UserRole.ADMIN);
        assertThat(entity.getStatusCode()).isEqualTo(UserStatus.ACTIVE);
        assertThat(result.getId()).isEqualTo("user-1");
        assertThat(result.getEmail()).isEqualTo(user.getEmail());
    }

    @Test
    void findUserByIdReturnsMappedDomainUser() {
        User user = user("user-2", UserRole.BUYER, UserStatus.BLOCKED);
        UserJpaEntity entity = userMapper.toEntity(user);
        when(userRepository.findById("user-2")).thenReturn(Optional.of(entity));

        Optional<User> result = userAdapter.findById("user-2");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("user-2");
        assertThat(result.get().getRole()).isEqualTo(UserRole.BUYER);
        assertThat(result.get().getStatus()).isEqualTo(UserStatus.BLOCKED);
        verify(userRepository).findById("user-2");
    }

    @Test
    void existsUserByRoleDelegatesWithCatalogType() {
        when(userRepository.existsByRoleCode(UserRole.ADMIN)).thenReturn(true);

        boolean result = userAdapter.existsByRole(UserRole.ADMIN);

        assertThat(result).isTrue();
        verify(userRepository).existsByRoleCode(UserRole.ADMIN);
    }

    @Test
    void saveBuyerBuildsEntityAndResolvesOwningUser() {
        Buyer buyer = buyer("buyer-1");
        when(buyerRepository.save(any(BuyerJpaEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(resolver.user(buyer.getUser().getId())).thenReturn(buyer.getUser());
        ArgumentCaptor<BuyerJpaEntity> captor = ArgumentCaptor.forClass(BuyerJpaEntity.class);

        Buyer result = buyerAdapter.save(buyer);

        verify(buyerRepository).save(captor.capture());
        BuyerJpaEntity entity = captor.getValue();
        assertThat(entity.getId()).isEqualTo("buyer-1");
        assertThat(entity.getUserId()).isEqualTo(buyer.getUser().getId());
        assertThat(entity.getCommercialStatusCode()).isEqualTo(CommercialStatus.ELIGIBLE);
        assertThat(result.getId()).isEqualTo("buyer-1");
        assertThat(result.getUser().getId()).isEqualTo(buyer.getUser().getId());
        assertThat(result.getMainAddress()).isEqualTo(buyer.getMainAddress());
    }

    @Test
    void findBuyerByIdDelegatesToResolverAndRepository() {
        Buyer buyer = buyer("buyer-2");
        BuyerJpaEntity entity = buyerMapper.toEntity(buyer);
        when(buyerRepository.findById("buyer-2")).thenReturn(Optional.of(entity));
        when(resolver.buyer(entity)).thenReturn(buyer);

        Optional<Buyer> result = buyerAdapter.findById("buyer-2");

        assertThat(result).contains(buyer);
        verify(buyerRepository).findById("buyer-2");
        verify(resolver).buyer(entity);
    }

    @Test
    void saveSellerBuildsEntityAndMapsUserTaxIdAndWarehouses() {
        Seller seller = seller("seller-1");
        when(sellerRepository.save(any(SellerJpaEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<SellerJpaEntity> captor = ArgumentCaptor.forClass(SellerJpaEntity.class);

        Seller result = sellerAdapter.save(seller);

        verify(sellerRepository).save(captor.capture());
        SellerJpaEntity entity = captor.getValue();
        assertThat(entity.getId()).isEqualTo("seller-1");
        assertThat(entity.getUserId()).isEqualTo(seller.getUser().getId());
        assertThat(entity.getTaxIdValue()).isEqualTo(seller.getTaxId().getValue());
        assertThat(result.getId()).isEqualTo("seller-1");
        assertThat(result.getTaxId()).isEqualTo(seller.getTaxId());
    }

    @Test
    void findSellerByIdDelegatesToResolverAndRepository() {
        Seller seller = seller("seller-2");
        SellerJpaEntity entity = sellerMapper.toEntity(seller);
        when(sellerRepository.findById("seller-2")).thenReturn(Optional.of(entity));
        when(resolver.seller(entity)).thenReturn(seller);

        Optional<Seller> result = sellerAdapter.findById("seller-2");

        assertThat(result).contains(seller);
        verify(sellerRepository).findById("seller-2");
        verify(resolver).seller(entity);
    }

    @Test
    void saveWarehouseBuildsEntityAndMapsOwnerAndResponsible() {
        Seller seller = seller("seller-wh");
        Warehouse warehouse = warehouse("warehouse-1", seller);
        when(warehouseRepository.save(any(WarehouseJpaEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<WarehouseJpaEntity> captor = ArgumentCaptor.forClass(WarehouseJpaEntity.class);

        Warehouse result = warehouseAdapter.save(warehouse);

        verify(warehouseRepository).save(captor.capture());
        WarehouseJpaEntity entity = captor.getValue();
        assertThat(entity.getId()).isEqualTo("warehouse-1");
        assertThat(entity.getName()).isEqualTo("Warehouse warehouse-1");
        assertThat(entity.getTypeCode()).isEqualTo(WarehouseType.SELLER);
        assertThat(entity.getOwnerSellerId()).isEqualTo(seller.getId());
        assertThat(entity.getResponsibleUserId()).isEqualTo(warehouse.getResponsible().getId());
        assertThat(result.getId()).isEqualTo("warehouse-1");
        assertThat(result.getLocation()).isEqualTo(warehouse.getLocation());
    }

    @Test
    void findWarehouseByIdDelegatesToResolverAndRepository() {
        Seller seller = seller("seller-wh-2");
        Warehouse warehouse = warehouse("warehouse-2", seller);
        WarehouseJpaEntity entity = warehouseMapper.toEntity(warehouse);
        when(warehouseRepository.findById("warehouse-2")).thenReturn(Optional.of(entity));
        when(resolver.warehouse(entity)).thenReturn(warehouse);

        Optional<Warehouse> result = warehouseAdapter.findById("warehouse-2");

        assertThat(result).contains(warehouse);
        verify(warehouseRepository).findById("warehouse-2");
        verify(resolver).warehouse(entity);
    }

    @Test
    void saveProductPersistsEntityAndReplacesVariants() {
        Seller seller = seller("seller-product");
        List<ProductVariant> variants = List.of(
                ProductVariant.of(VariantType.COLOR, "Black"),
                ProductVariant.of(VariantType.SIZE, "XL"));
        Product product = product("product-1", seller, "119.00", variants);
        ProductVariantJpaEntity existing = new ProductVariantJpaEntity();
        existing.setId("old-variant");
        when(productRepository.save(any(ProductJpaEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(variantRepository.findByProductId("product-1")).thenReturn(List.of(existing));
        ArgumentCaptor<ProductJpaEntity> productCaptor =
                ArgumentCaptor.forClass(ProductJpaEntity.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ProductVariantJpaEntity>> variantCaptor =
                ArgumentCaptor.forClass(List.class);

        Product result = productAdapter.save(product);

        verify(productRepository).save(productCaptor.capture());
        ProductJpaEntity entity = productCaptor.getValue();
        assertThat(entity.getId()).isEqualTo("product-1");
        assertThat(entity.getSellerId()).isEqualTo(seller.getId());
        assertThat(entity.getPriceAmount()).isEqualByComparingTo("119.00");
        assertThat(entity.getPriceCurrency()).isEqualTo("COP");
        assertThat(entity.getStatusCode()).isEqualTo(ProductStatus.PUBLISHED);
        verify(variantRepository).findByProductId("product-1");
        verify(variantRepository).deleteAll(List.of(existing));
        verify(variantRepository).saveAll(variantCaptor.capture());
        assertThat(variantCaptor.getValue()).hasSize(2);
        assertThat(variantCaptor.getValue()).extracting(ProductVariantJpaEntity::getProductId)
                .containsOnly("product-1");
        assertThat(variantCaptor.getValue()).extracting(ProductVariantJpaEntity::getValue)
                .containsExactly("Black", "XL");
        assertThat(result.getId()).isEqualTo("product-1");
        assertThat(result.getVariants()).containsExactlyElementsOf(variants);
    }

    @Test
    void findProductByIdDelegatesToResolverAndRepository() {
        Seller seller = seller("seller-product-2");
        Product product = product("product-2", seller, "50.00", List.of());
        ProductJpaEntity entity = productMapper.toEntity(product);
        when(productRepository.findById("product-2")).thenReturn(Optional.of(entity));
        when(resolver.product(entity)).thenReturn(product);

        Optional<Product> result = productAdapter.findById("product-2");

        assertThat(result).contains(product);
        verify(productRepository).findById("product-2");
        verify(resolver).product(entity);
    }

    @Test
    void saveInventoryBuildsEntityAndMapsQuantities() {
        Seller seller = seller("seller-inventory");
        Warehouse warehouse = warehouse("warehouse-inventory", seller);
        Product product = product("product-inventory", seller, "80.00", List.of());
        Inventory inventory = inventory("inventory-1", product, warehouse);
        when(inventoryRepository.save(any(InventoryJpaEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<InventoryJpaEntity> captor = ArgumentCaptor.forClass(InventoryJpaEntity.class);

        Inventory result = inventoryAdapter.save(inventory);

        verify(inventoryRepository).save(captor.capture());
        InventoryJpaEntity entity = captor.getValue();
        assertThat(entity.getId()).isEqualTo("inventory-1");
        assertThat(entity.getProductId()).isEqualTo("product-inventory");
        assertThat(entity.getWarehouseId()).isEqualTo("warehouse-inventory");
        assertThat(entity.getAvailableQuantity()).isEqualTo(10);
        assertThat(entity.getReservedQuantity()).isEqualTo(2);
        assertThat(entity.getConditionCode()).isEqualTo(InventoryCondition.GOOD);
        assertThat(result.getId()).isEqualTo("inventory-1");
        assertThat(result.getAvailableQuantity()).isEqualTo(10);
    }

    @Test
    void findInventoryByProductAndWarehouseDelegatesWithBothArguments() {
        Seller seller = seller("seller-inventory-2");
        Warehouse warehouse = warehouse("warehouse-inventory-2", seller);
        Product product = product("product-inventory-2", seller, "80.00", List.of());
        Inventory inventory = inventory("inventory-2", product, warehouse);
        InventoryJpaEntity entity = inventoryMapper.toEntity(inventory);
        when(inventoryRepository.findByProductIdAndWarehouseId(
                "product-inventory-2", "warehouse-inventory-2")).thenReturn(Optional.of(entity));
        when(resolver.inventory(entity)).thenReturn(inventory);

        Optional<Inventory> result = inventoryAdapter.findByProductIdAndWarehouseId(
                "product-inventory-2", "warehouse-inventory-2");

        assertThat(result).contains(inventory);
        verify(inventoryRepository).findByProductIdAndWarehouseId(
                "product-inventory-2", "warehouse-inventory-2");
        verify(resolver).inventory(entity);
    }

    @Test
    void saveInventoryMovementBuildsEntityAndMapsActorAndTimestamp() {
        Seller seller = seller("seller-movement");
        Warehouse warehouse = warehouse("warehouse-movement", seller);
        Product product = product("product-movement", seller, "80.00", List.of());
        Inventory inventory = inventory("inventory-movement", product, warehouse);
        User performer = user("performer-1", UserRole.LOGISTIC_OPERATOR, UserStatus.ACTIVE);
        InventoryMovement movement = movement("movement-1", inventory, performer);
        when(movementRepository.save(any(InventoryMovementJpaEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<InventoryMovementJpaEntity> captor =
                ArgumentCaptor.forClass(InventoryMovementJpaEntity.class);

        InventoryMovement result = movementAdapter.save(movement);

        verify(movementRepository).save(captor.capture());
        InventoryMovementJpaEntity entity = captor.getValue();
        assertThat(entity.getId()).isEqualTo("movement-1");
        assertThat(entity.getInventoryId()).isEqualTo("inventory-movement");
        assertThat(entity.getTypeCode()).isEqualTo(MovementType.ADJUSTMENT);
        assertThat(entity.getQuantity()).isEqualTo(5);
        assertThat(entity.getPerformedByUserId()).isEqualTo("performer-1");
        assertThat(entity.getOccurredAt()).isEqualTo(NOW);
        assertThat(result.getId()).isEqualTo("movement-1");
        assertThat(result.getPerformedBy().getId()).isEqualTo("performer-1");
    }

    @Test
    void findInventoryMovementsResolvesInventoryAndUserDependencies() {
        Seller seller = seller("seller-movement-2");
        Warehouse warehouse = warehouse("warehouse-movement-2", seller);
        Product product = product("product-movement-2", seller, "80.00", List.of());
        Inventory inventory = inventory("inventory-movement-2", product, warehouse);
        User performer = user("performer-2", UserRole.ADMIN, UserStatus.ACTIVE);
        InventoryMovement movement = movement("movement-2", inventory, performer);
        InventoryMovementJpaEntity entity = movementMapper.toEntity(movement);
        when(movementRepository.findByInventoryId("inventory-movement-2"))
                .thenReturn(List.of(entity));
        when(resolver.inventory("inventory-movement-2")).thenReturn(inventory);
        when(resolver.user("performer-2")).thenReturn(performer);

        List<InventoryMovement> result = movementAdapter.findByInventoryId("inventory-movement-2");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("movement-2");
        assertThat(result.get(0).getInventory().getId()).isEqualTo("inventory-movement-2");
        assertThat(result.get(0).getPerformedBy().getId()).isEqualTo("performer-2");
        verify(movementRepository).findByInventoryId("inventory-movement-2");
        verify(resolver).inventory("inventory-movement-2");
        verify(resolver).user("performer-2");
    }

    @Test
    void saveCartPersistsEntityAndDeletesThenSavesItems() {
        Seller seller = seller("seller-cart");
        Product first = product("product-cart-1", seller, "10.00", List.of());
        Product second = product("product-cart-2", seller, "20.00", List.of());
        Cart cart = cart("cart-1", buyer("buyer-cart"),
                List.of(CartItem.reconstitute(first, 2, Money.cop(new BigDecimal("10.00"))),
                        CartItem.reconstitute(second, 3, Money.cop(new BigDecimal("20.00")))));
        when(cartRepository.save(any(CartJpaEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CartItemJpaEntity>> itemCaptor =
                ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<CartJpaEntity> cartCaptor = ArgumentCaptor.forClass(CartJpaEntity.class);

        Cart result = cartAdapter.save(cart);

        verify(cartRepository).save(cartCaptor.capture());
        assertThat(cartCaptor.getValue().getId()).isEqualTo("cart-1");
        assertThat(cartCaptor.getValue().getBuyerId()).isEqualTo(cart.getBuyer().getId());
        verify(cartItemRepository).deleteByCartId("cart-1");
        verify(cartItemRepository).saveAll(itemCaptor.capture());
        assertThat(itemCaptor.getValue()).hasSize(2);
        assertThat(itemCaptor.getValue()).extracting(CartItemJpaEntity::getCartId)
                .containsOnly("cart-1");
        assertThat(itemCaptor.getValue()).extracting(CartItemJpaEntity::getProductId)
                .containsExactly("product-cart-1", "product-cart-2");
        assertThat(itemCaptor.getValue()).extracting(CartItemJpaEntity::getQuantity)
                .containsExactly(2, 3);
        assertThat(result.getId()).isEqualTo("cart-1");
        assertThat(result.getItems()).hasSize(2);
    }

    @Test
    void findCartByBuyerIdResolvesBuyerAndItems() {
        Seller seller = seller("seller-cart-2");
        Product product = product("product-cart-3", seller, "30.00", List.of());
        Buyer buyer = buyer("buyer-cart-2");
        Cart cart = cart("cart-2", buyer,
                List.of(CartItem.reconstitute(product, 1, Money.cop(new BigDecimal("30.00")))));
        CartJpaEntity entity = cartMapper.toEntity(cart);
        when(cartRepository.findByBuyerId("buyer-cart-2")).thenReturn(Optional.of(entity));
        when(resolver.buyer("buyer-cart-2")).thenReturn(buyer);
        when(resolver.cartItems("cart-2")).thenReturn(cart.getItems());

        Optional<Cart> result = cartAdapter.findByBuyerId("buyer-cart-2");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("cart-2");
        assertThat(result.get().getItems()).hasSize(1);
        verify(cartRepository).findByBuyerId("buyer-cart-2");
        verify(resolver).buyer("buyer-cart-2");
        verify(resolver).cartItems("cart-2");
    }

    @Test
    void saveOrderPersistsEntityAndReplacesItemsAndAllocations() {
        Seller seller = seller("seller-order");
        Product first = product("product-order-1", seller, "100.00", List.of());
        Product second = product("product-order-2", seller, "50.00", List.of());
        List<CartItem> items = List.of(
                CartItem.reconstitute(first, 3, Money.cop(new BigDecimal("100.00"))),
                CartItem.reconstitute(second, 1, Money.cop(new BigDecimal("50.00"))));
        List<StockAllocation> allocations = List.of(
                StockAllocation.of("product-order-1", "warehouse-order-1", 3),
                StockAllocation.of("product-order-2", "warehouse-order-2", 1));
        Order order = order("order-1", buyer("buyer-order"), items,
                Money.cop(new BigDecimal("350.00")), allocations, null);
        OrderItemJpaEntity existingItem = new OrderItemJpaEntity();
        existingItem.setId("old-item");
        OrderAllocationJpaEntity existingAllocation = new OrderAllocationJpaEntity();
        existingAllocation.setId("old-allocation");
        when(orderRepository.save(any(OrderJpaEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(orderItemRepository.findByOrderId("order-1")).thenReturn(List.of(existingItem));
        when(orderAllocationRepository.findByOrderId("order-1"))
                .thenReturn(List.of(existingAllocation));
        ArgumentCaptor<OrderJpaEntity> orderCaptor = ArgumentCaptor.forClass(OrderJpaEntity.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<OrderItemJpaEntity>> itemCaptor =
                ArgumentCaptor.forClass(List.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<OrderAllocationJpaEntity>> allocationCaptor =
                ArgumentCaptor.forClass(List.class);

        Order result = orderAdapter.save(order);

        verify(orderRepository).save(orderCaptor.capture());
        OrderJpaEntity entity = orderCaptor.getValue();
        assertThat(entity.getId()).isEqualTo("order-1");
        assertThat(entity.getBuyerId()).isEqualTo("buyer-order");
        assertThat(entity.getStatusCode()).isEqualTo(OrderStatus.PAID);
        assertThat(entity.getTotalAmount()).isEqualByComparingTo("350.00");
        assertThat(entity.getTotalCurrency()).isEqualTo("COP");
        assertThat(entity.getPaymentStatusCode()).isEqualTo(PaymentStatus.CONFIRMED);
        assertThat(entity.getCreatedAt()).isEqualTo(NOW);
        verify(orderItemRepository).deleteAll(List.of(existingItem));
        verify(orderAllocationRepository).deleteAll(List.of(existingAllocation));
        verify(orderItemRepository).saveAll(itemCaptor.capture());
        verify(orderAllocationRepository).saveAll(allocationCaptor.capture());
        assertThat(itemCaptor.getValue()).hasSize(2);
        assertThat(itemCaptor.getValue()).extracting(OrderItemJpaEntity::getOrderId)
                .containsOnly("order-1");
        assertThat(allocationCaptor.getValue()).hasSize(2);
        assertThat(allocationCaptor.getValue())
                .extracting(OrderAllocationJpaEntity::getOrderId)
                .containsOnly("order-1");
        assertThat(result.getId()).isEqualTo("order-1");
        assertThat(result.getItems()).hasSize(2);
        assertThat(result.getAllocations()).containsExactlyElementsOf(allocations);
    }

    @Test
    void findOrderByIdDelegatesToResolverAndRepository() {
        Seller seller = seller("seller-order-2");
        Product product = product("product-order-3", seller, "75.00", List.of());
        Order order = order("order-2", buyer("buyer-order-2"),
                List.of(CartItem.reconstitute(product, 1, Money.cop(new BigDecimal("75.00")))),
                Money.cop(new BigDecimal("75.00")), List.of(), null);
        OrderJpaEntity entity = orderMapper.toEntity(order);
        when(orderRepository.findById("order-2")).thenReturn(Optional.of(entity));
        when(resolver.order(entity)).thenReturn(order);

        Optional<Order> result = orderAdapter.findById("order-2");

        assertThat(result).contains(order);
        verify(orderRepository).findById("order-2");
        verify(resolver).order(entity);
    }

    @Test
    void saveInvoicePersistsMoneyStatusAndOrderReference() {
        Order order = order("order-invoice", buyer("buyer-invoice"), List.of(),
                Money.cop(new BigDecimal("350.00")), List.of(), null);
        Invoice invoice = Invoice.reconstitute("invoice-1", order, "INV-001",
                Money.cop(new BigDecimal("294.12")), Money.cop(new BigDecimal("55.88")),
                Money.cop(new BigDecimal("350.00")), InvoiceStatus.ISSUED, NOW);
        when(invoiceRepository.save(any(InvoiceJpaEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<InvoiceJpaEntity> captor = ArgumentCaptor.forClass(InvoiceJpaEntity.class);

        Invoice result = invoiceAdapter.save(invoice);

        verify(invoiceRepository).save(captor.capture());
        InvoiceJpaEntity entity = captor.getValue();
        assertThat(entity.getId()).isEqualTo("invoice-1");
        assertThat(entity.getOrderId()).isEqualTo("order-invoice");
        assertThat(entity.getNumber()).isEqualTo("INV-001");
        assertThat(entity.getSubtotalAmount()).isEqualByComparingTo("294.12");
        assertThat(entity.getTaxesAmount()).isEqualByComparingTo("55.88");
        assertThat(entity.getTotalAmount()).isEqualByComparingTo("350.00");
        assertThat(entity.getTotalCurrency()).isEqualTo("COP");
        assertThat(entity.getStatusCode()).isEqualTo(InvoiceStatus.ISSUED);
        assertThat(entity.getIssuedAt()).isEqualTo(NOW);
        assertThat(result.getId()).isEqualTo("invoice-1");
        assertThat(result.getTotal()).isEqualTo(invoice.getTotal());
    }

    @Test
    void findInvoiceByIdDelegatesToResolverAndRepository() {
        Order order = order("order-invoice-2", buyer("buyer-invoice-2"), List.of(),
                Money.cop(new BigDecimal("100.00")), List.of(), null);
        Invoice invoice = Invoice.reconstitute("invoice-2", order, "INV-002",
                Money.cop(new BigDecimal("100.00")), Money.cop(BigDecimal.ZERO),
                Money.cop(new BigDecimal("100.00")), InvoiceStatus.ISSUED, NOW);
        InvoiceJpaEntity entity = invoiceMapper.toEntity(invoice);
        when(invoiceRepository.findById("invoice-2")).thenReturn(Optional.of(entity));
        when(resolver.invoice(entity)).thenReturn(invoice);

        Optional<Invoice> result = invoiceAdapter.findById("invoice-2");

        assertThat(result).contains(invoice);
        verify(invoiceRepository).findById("invoice-2");
        verify(resolver).invoice(entity);
    }

    @Test
    void saveShipmentPersistsOrderOriginDestinationAndTracking() {
        Seller seller = seller("seller-shipment");
        Warehouse origin = warehouse("warehouse-shipment", seller);
        Order order = order("order-shipment", buyer("buyer-shipment"), List.of(),
                Money.cop(new BigDecimal("150.00")), List.of(), null);
        Shipment shipment = Shipment.reconstitute("shipment-1", order, origin,
                address("Delivery"), "TRK-1", ShipmentStatus.IN_TRANSIT);
        when(shipmentRepository.save(any(ShipmentJpaEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<ShipmentJpaEntity> captor = ArgumentCaptor.forClass(ShipmentJpaEntity.class);

        Shipment result = shipmentAdapter.save(shipment);

        verify(shipmentRepository).save(captor.capture());
        ShipmentJpaEntity entity = captor.getValue();
        assertThat(entity.getId()).isEqualTo("shipment-1");
        assertThat(entity.getOrderId()).isEqualTo("order-shipment");
        assertThat(entity.getOriginWarehouseId()).isEqualTo("warehouse-shipment");
        assertThat(entity.getDestination().getStreet()).isEqualTo("Delivery");
        assertThat(entity.getTrackingNumber()).isEqualTo("TRK-1");
        assertThat(entity.getStatusCode()).isEqualTo(ShipmentStatus.IN_TRANSIT);
        assertThat(result.getId()).isEqualTo("shipment-1");
        assertThat(result.getStatus()).isEqualTo(ShipmentStatus.IN_TRANSIT);
    }

    @Test
    void findShipmentByIdResolvesOrderAndOriginWarehouse() {
        Seller seller = seller("seller-shipment-2");
        Warehouse origin = warehouse("warehouse-shipment-2", seller);
        Order order = order("order-shipment-2", buyer("buyer-shipment-2"), List.of(),
                Money.cop(new BigDecimal("90.00")), List.of(), null);
        Shipment shipment = Shipment.reconstitute("shipment-2", order, origin,
                address("Delivery 2"), "TRK-2", ShipmentStatus.PENDING);
        ShipmentJpaEntity entity = shipmentMapper.toEntity(shipment);
        when(shipmentRepository.findById("shipment-2")).thenReturn(Optional.of(entity));
        when(resolver.orderWithoutInvoice("order-shipment-2")).thenReturn(order);
        when(resolver.warehouse("warehouse-shipment-2")).thenReturn(origin);

        Optional<Shipment> result = shipmentAdapter.findById("shipment-2");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("shipment-2");
        assertThat(result.get().getOrder().getId()).isEqualTo("order-shipment-2");
        assertThat(result.get().getOriginWarehouse().getId()).isEqualTo("warehouse-shipment-2");
        verify(shipmentRepository).findById("shipment-2");
        verify(resolver).orderWithoutInvoice("order-shipment-2");
        verify(resolver).warehouse("warehouse-shipment-2");
    }

    @Test
    void saveReturnPersistsReasonStatusAndOrderReference() {
        Order order = order("order-return", buyer("buyer-return"), List.of(),
                Money.cop(new BigDecimal("60.00")), List.of(), null);
        Return returnRequest = Return.reconstitute("return-1", order,
                "Damaged on arrival", ReturnStatus.APPROVED, NOW);
        when(returnRepository.save(any(ReturnJpaEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<ReturnJpaEntity> captor = ArgumentCaptor.forClass(ReturnJpaEntity.class);

        Return result = returnAdapter.save(returnRequest);

        verify(returnRepository).save(captor.capture());
        ReturnJpaEntity entity = captor.getValue();
        assertThat(entity.getId()).isEqualTo("return-1");
        assertThat(entity.getOrderId()).isEqualTo("order-return");
        assertThat(entity.getReason()).isEqualTo("Damaged on arrival");
        assertThat(entity.getStatusCode()).isEqualTo(ReturnStatus.APPROVED);
        assertThat(entity.getRequestedAt()).isEqualTo(NOW);
        assertThat(result.getId()).isEqualTo("return-1");
        assertThat(result.getStatus()).isEqualTo(ReturnStatus.APPROVED);
    }

    @Test
    void findReturnByIdDelegatesToResolverAndRepository() {
        Order order = order("order-return-2", buyer("buyer-return-2"), List.of(),
                Money.cop(new BigDecimal("45.00")), List.of(), null);
        Return returnRequest = Return.reconstitute("return-2", order,
                "Wrong size", ReturnStatus.COMPLETED, NOW);
        ReturnJpaEntity entity = returnMapper.toEntity(returnRequest);
        when(returnRepository.findById("return-2")).thenReturn(Optional.of(entity));
        when(resolver.returnRequest(entity)).thenReturn(returnRequest);

        Optional<Return> result = returnAdapter.findById("return-2");

        assertThat(result).contains(returnRequest);
        verify(returnRepository).findById("return-2");
        verify(resolver).returnRequest(entity);
    }

    @Test
    void saveRefundPersistsAmountStatusAndReturnReference() {
        Order order = order("order-refund", buyer("buyer-refund"), List.of(),
                Money.cop(new BigDecimal("90.00")), List.of(), null);
        Return returnRequest = Return.reconstitute("return-refund", order,
                "Broken item", ReturnStatus.APPROVED, NOW);
        Refund refund = Refund.reconstitute("refund-1", returnRequest,
                Money.cop(new BigDecimal("40.00")), RefundStatus.PROCESSED);
        when(refundRepository.save(any(RefundJpaEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        ArgumentCaptor<RefundJpaEntity> captor = ArgumentCaptor.forClass(RefundJpaEntity.class);

        Refund result = refundAdapter.save(refund);

        verify(refundRepository).save(captor.capture());
        RefundJpaEntity entity = captor.getValue();
        assertThat(entity.getId()).isEqualTo("refund-1");
        assertThat(entity.getReturnId()).isEqualTo("return-refund");
        assertThat(entity.getAmountAmount()).isEqualByComparingTo("40.00");
        assertThat(entity.getAmountCurrency()).isEqualTo("COP");
        assertThat(entity.getStatusCode()).isEqualTo(RefundStatus.PROCESSED);
        assertThat(result.getId()).isEqualTo("refund-1");
        assertThat(result.getAmount()).isEqualTo(refund.getAmount());
    }

    @Test
    void findRefundByIdResolvesReturnAndMapsRefund() {
        Order order = order("order-refund-2", buyer("buyer-refund-2"), List.of(),
                Money.cop(new BigDecimal("120.00")), List.of(), null);
        Return returnRequest = Return.reconstitute("return-refund-2", order,
                "Late delivery", ReturnStatus.APPROVED, NOW);
        Refund refund = Refund.reconstitute("refund-2", returnRequest,
                Money.cop(new BigDecimal("25.00")), RefundStatus.PENDING);
        RefundJpaEntity entity = refundMapper.toEntity(refund);
        when(refundRepository.findById("refund-2")).thenReturn(Optional.of(entity));
        when(resolver.returnRequest("return-refund-2")).thenReturn(returnRequest);

        Optional<Refund> result = refundAdapter.findById("refund-2");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("refund-2");
        assertThat(result.get().getReturnRequest().getId()).isEqualTo("return-refund-2");
        assertThat(result.get().getAmount()).isEqualTo(refund.getAmount());
        verify(refundRepository).findById("refund-2");
        verify(resolver).returnRequest("return-refund-2");
    }

    private static Address address(String street) {
        return Address.of(street, "Bogota", "Cundinamarca", "110111", "Colombia");
    }

    private static User user(String id, UserRole role, UserStatus status) {
        return User.reconstitute(id, FullName.of("User " + id),
                Email.of(id + "@nexus.test"),
                IdentityDocument.of("DOC" + String.format("%08X", id.hashCode())),
                role, status, PasswordHash.of("hash-" + id), 0L);
    }

    private static Buyer buyer(String id) {
        return Buyer.reconstitute(id, user(id + "-user", UserRole.BUYER, UserStatus.ACTIVE),
                address("Main Street"), List.of(address("Second Street")),
                CommercialStatus.ELIGIBLE);
    }

    private static Seller seller(String id) {
        return Seller.reconstitute(id, user(id + "-user", UserRole.SELLER, UserStatus.ACTIVE),
                TaxId.of("900123456-7"), List.of());
    }

    private static Warehouse warehouse(String id, Seller seller) {
        return Warehouse.reconstitute(id, "Warehouse " + id, address("Warehouse Street"),
                WarehouseType.SELLER, seller,
                user(id + "-responsible", UserRole.ADMIN, UserStatus.ACTIVE));
    }

    private static Product product(String id, Seller seller, String price,
                                   List<ProductVariant> variants) {
        return Product.reconstitute(id, seller, "Product " + id, "Description " + id,
                ProductType.PHYSICAL, ProductStatus.PUBLISHED,
                Money.cop(new BigDecimal(price)), variants);
    }

    private static Inventory inventory(String id, Product product, Warehouse warehouse) {
        return Inventory.reconstitute(id, product, warehouse, 10, 2, InventoryCondition.GOOD);
    }

    private static InventoryMovement movement(String id, Inventory inventory, User performer) {
        return InventoryMovement.reconstitute(id, inventory, MovementType.ADJUSTMENT, 5,
                performer, NOW);
    }

    private static Cart cart(String id, Buyer buyer, List<CartItem> items) {
        return Cart.reconstitute(id, buyer, items);
    }

    private static Order order(String id, Buyer buyer, List<CartItem> items, Money total,
                               List<StockAllocation> allocations, Invoice invoice) {
        return Order.reconstitute(id, buyer, items, OrderStatus.PAID, total,
                PaymentStatus.CONFIRMED, allocations, address("Delivery Street"),
                invoice, NOW);
    }
}
