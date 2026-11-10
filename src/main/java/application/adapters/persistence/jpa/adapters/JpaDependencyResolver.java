package application.adapters.persistence.jpa.adapters;

import application.adapters.persistence.jpa.entities.BuyerJpaEntity;
import application.adapters.persistence.jpa.entities.InventoryJpaEntity;
import application.adapters.persistence.jpa.entities.InvoiceJpaEntity;
import application.adapters.persistence.jpa.entities.OrderAllocationJpaEntity;
import application.adapters.persistence.jpa.entities.OrderItemJpaEntity;
import application.adapters.persistence.jpa.entities.OrderJpaEntity;
import application.adapters.persistence.jpa.entities.ProductJpaEntity;
import application.adapters.persistence.jpa.entities.ProductVariantJpaEntity;
import application.adapters.persistence.jpa.entities.ReturnJpaEntity;
import application.adapters.persistence.jpa.entities.SellerJpaEntity;
import application.adapters.persistence.jpa.entities.UserJpaEntity;
import application.adapters.persistence.jpa.entities.WarehouseJpaEntity;
import application.adapters.persistence.jpa.mappers.BuyerJpaMapper;
import application.adapters.persistence.jpa.mappers.CartJpaMapper;
import application.adapters.persistence.jpa.mappers.InventoryJpaMapper;
import application.adapters.persistence.jpa.mappers.InvoiceJpaMapper;
import application.adapters.persistence.jpa.mappers.OrderJpaMapper;
import application.adapters.persistence.jpa.mappers.ProductJpaMapper;
import application.adapters.persistence.jpa.mappers.ReturnJpaMapper;
import application.adapters.persistence.jpa.mappers.SellerJpaMapper;
import application.adapters.persistence.jpa.mappers.UserJpaMapper;
import application.adapters.persistence.jpa.mappers.WarehouseJpaMapper;
import application.adapters.persistence.jpa.repositories.BuyerJpaRepository;
import application.adapters.persistence.jpa.repositories.CartItemJpaRepository;
import application.adapters.persistence.jpa.repositories.InventoryJpaRepository;
import application.adapters.persistence.jpa.repositories.InvoiceJpaRepository;
import application.adapters.persistence.jpa.repositories.OrderAllocationJpaRepository;
import application.adapters.persistence.jpa.repositories.OrderItemJpaRepository;
import application.adapters.persistence.jpa.repositories.OrderJpaRepository;
import application.adapters.persistence.jpa.repositories.ProductJpaRepository;
import application.adapters.persistence.jpa.repositories.ProductVariantJpaRepository;
import application.adapters.persistence.jpa.repositories.ReturnJpaRepository;
import application.adapters.persistence.jpa.repositories.SellerJpaRepository;
import application.adapters.persistence.jpa.repositories.UserJpaRepository;
import application.adapters.persistence.jpa.repositories.WarehouseJpaRepository;
import application.domain.exceptions.seller.SellerNotFoundException;
import application.domain.exceptions.user.UserNotFoundException;
import application.domain.models.Buyer;
import application.domain.models.CartItem;
import application.domain.models.Inventory;
import application.domain.models.Invoice;
import application.domain.models.Order;
import application.domain.models.Product;
import application.domain.models.Return;
import application.domain.models.Seller;
import application.domain.models.User;
import application.domain.models.Warehouse;
import application.domain.valueobjects.ProductVariant;
import application.domain.valueobjects.StockAllocation;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaDependencyResolver {

    private final UserJpaRepository userRepository;
    private final UserJpaMapper userMapper;
    private final BuyerJpaRepository buyerRepository;
    private final BuyerJpaMapper buyerMapper;
    private final SellerJpaRepository sellerRepository;
    private final SellerJpaMapper sellerMapper;
    private final WarehouseJpaRepository warehouseRepository;
    private final WarehouseJpaMapper warehouseMapper;
    private final ProductJpaRepository productRepository;
    private final ProductJpaMapper productMapper;
    private final ProductVariantJpaRepository productVariantRepository;
    private final InventoryJpaRepository inventoryRepository;
    private final InventoryJpaMapper inventoryMapper;
    private final CartItemJpaRepository cartItemRepository;
    private final CartJpaMapper cartJpaMapper;
    private final OrderJpaRepository orderRepository;
    private final OrderItemJpaRepository orderItemRepository;
    private final OrderAllocationJpaRepository orderAllocationRepository;
    private final OrderJpaMapper orderJpaMapper;
    private final InvoiceJpaRepository invoiceRepository;
    private final InvoiceJpaMapper invoiceJpaMapper;
    private final ReturnJpaRepository returnRepository;
    private final ReturnJpaMapper returnJpaMapper;

    public User user(String id) {
        return userRepository.findById(id)
                .map(userMapper::toDomain)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + id));
    }

    public Buyer buyer(String id) {
        return buyerRepository.findById(id)
                .map(this::buyer)
                .orElseThrow(() -> new IllegalStateException("Buyer not found: " + id));
    }

    public Buyer buyer(BuyerJpaEntity entity) {
        return buyerMapper.toDomain(entity, user(entity.getUserId()));
    }

    public Seller seller(String id) {
        SellerJpaEntity entity = sellerRepository.findById(id)
                .orElseThrow(() -> new SellerNotFoundException("Seller not found: " + id));
        return seller(entity);
    }

    public Seller seller(SellerJpaEntity entity) {
        User user = user(entity.getUserId());
        Seller shallow = sellerMapper.toDomain(entity, user, List.of());
        List<Warehouse> warehouses = warehouseRepository.findByOwnerSellerId(entity.getId()).stream()
                .map(warehouse -> warehouse(warehouse, shallow))
                .toList();
        return sellerMapper.toDomain(entity, user, warehouses);
    }

    public Seller sellerShallow(String id) {
        return sellerRepository.findById(id)
                .map(this::sellerShallow)
                .orElseThrow(() -> new SellerNotFoundException("Seller not found: " + id));
    }

    public Seller sellerShallow(SellerJpaEntity entity) {
        return sellerMapper.toDomain(entity, user(entity.getUserId()), List.of());
    }

    public Warehouse warehouse(String id) {
        return warehouseRepository.findById(id)
                .map(this::warehouse)
                .orElseThrow(() -> new IllegalStateException("Warehouse not found: " + id));
    }

    public Warehouse warehouse(WarehouseJpaEntity entity) {
        Seller owner = entity.getOwnerSellerId() == null ? null : sellerShallow(entity.getOwnerSellerId());
        return warehouse(entity, owner);
    }

    private Warehouse warehouse(WarehouseJpaEntity entity, Seller owner) {
        return warehouseMapper.toDomain(entity, owner, user(entity.getResponsibleUserId()));
    }

    public Product product(String id) {
        return productRepository.findById(id)
                .map(this::product)
                .orElseThrow(() -> new IllegalStateException("Product not found: " + id));
    }

    public Product product(ProductJpaEntity entity) {
        return productMapper.toDomain(entity, sellerShallow(entity.getSellerId()), variants(entity.getId()));
    }

    public Product productWithWarehouses(ProductJpaEntity entity) {
        return productMapper.toDomain(entity, seller(entity.getSellerId()), variants(entity.getId()));
    }

    public List<ProductVariant> variants(String productId) {
        return productVariantRepository.findByProductId(productId).stream()
                .map(productMapper::toDomain)
                .toList();
    }

    public Inventory inventory(String id) {
        return inventoryRepository.findById(id)
                .map(this::inventory)
                .orElseThrow(() -> new IllegalStateException("Inventory not found: " + id));
    }

    public Inventory inventory(InventoryJpaEntity entity) {
        return inventoryMapper.toDomain(
                entity,
                product(entity.getProductId()),
                warehouse(entity.getWarehouseId()));
    }

    public List<Inventory> inventories(List<InventoryJpaEntity> entities) {
        if (entities.isEmpty()) {
            return List.of();
        }
        Set<String> productIds = entities.stream()
                .map(InventoryJpaEntity::getProductId)
                .collect(Collectors.toSet());
        Set<String> warehouseIds = entities.stream()
                .map(InventoryJpaEntity::getWarehouseId)
                .collect(Collectors.toSet());
        Map<String, Product> products = productsByIds(productIds);
        Map<String, Warehouse> warehouses = warehousesByIds(warehouseIds);
        return entities.stream()
                .map(entity -> inventoryMapper.toDomain(
                        entity,
                        requireProduct(products, entity.getProductId()),
                        requireWarehouse(warehouses, entity.getWarehouseId())))
                .toList();
    }

    public List<CartItem> cartItems(String cartId) {
        return cartItemRepository.findByCartId(cartId).stream()
                .map(item -> cartJpaMapper.toDomain(item, product(item.getProductId())))
                .toList();
    }

    public Order order(String id) {
        return orderRepository.findById(id)
                .map(this::order)
                .orElseThrow(() -> new IllegalStateException("Order not found: " + id));
    }

    public Order order(OrderJpaEntity entity) {
        Order base = orderWithoutInvoice(entity);
        Invoice invoice = invoiceRepository.findByOrderId(entity.getId())
                .map(value -> invoiceJpaMapper.toDomain(value, base))
                .orElse(null);
        if (invoice == null) {
            return base;
        }
        return orderJpaMapper.toDomain(
                entity,
                base.getBuyer(),
                base.getItems(),
                base.getAllocations(),
                invoice);
    }

    public List<Order> orders(List<OrderJpaEntity> entities) {
        if (entities.isEmpty()) {
            return List.of();
        }
        Map<String, Order> baseById = ordersBase(entities).stream()
                .collect(Collectors.toMap(Order::getId, Function.identity()));
        Set<String> orderIds = entities.stream()
                .map(OrderJpaEntity::getId)
                .collect(Collectors.toSet());
        Map<String, InvoiceJpaEntity> invoicesByOrderId = invoiceRepository.findByOrderIdIn(orderIds).stream()
                .collect(Collectors.toMap(InvoiceJpaEntity::getOrderId, Function.identity()));
        return entities.stream()
                .map(entity -> {
                    Order base = requireOrder(baseById, entity.getId());
                    InvoiceJpaEntity invoiceEntity = invoicesByOrderId.get(entity.getId());
                    if (invoiceEntity == null) {
                        return base;
                    }
                    return orderJpaMapper.toDomain(
                            entity,
                            base.getBuyer(),
                            base.getItems(),
                            base.getAllocations(),
                            invoiceJpaMapper.toDomain(invoiceEntity, base));
                })
                .toList();
    }

    public Order orderWithoutInvoice(String id) {
        return orderRepository.findById(id)
                .map(this::orderWithoutInvoice)
                .orElseThrow(() -> new IllegalStateException("Order not found: " + id));
    }

    public Order orderWithoutInvoice(OrderJpaEntity entity) {
        return orderJpaMapper.toDomain(
                entity,
                buyer(entity.getBuyerId()),
                orderItems(entity.getId()),
                allocations(entity.getId()),
                null);
    }

    public List<Order> ordersWithoutInvoice(List<OrderJpaEntity> entities) {
        return ordersBase(entities);
    }

    public List<Order> ordersByIds(Collection<String> orderIds) {
        if (orderIds.isEmpty()) {
            return List.of();
        }
        return ordersBase(orderRepository.findAllById(orderIds));
    }

    private List<Order> ordersBase(List<OrderJpaEntity> entities) {
        if (entities.isEmpty()) {
            return List.of();
        }
        Set<String> orderIds = entities.stream()
                .map(OrderJpaEntity::getId)
                .collect(Collectors.toSet());
        Map<String, Buyer> buyers = buyersByIds(entities.stream()
                .map(OrderJpaEntity::getBuyerId)
                .collect(Collectors.toSet()));
        Map<String, List<OrderItemJpaEntity>> itemEntitiesByOrderId =
                orderItemRepository.findByOrderIdIn(orderIds).stream()
                        .collect(Collectors.groupingBy(OrderItemJpaEntity::getOrderId));
        Map<String, Product> products = productsByIds(itemEntitiesByOrderId.values().stream()
                .flatMap(List::stream)
                .map(OrderItemJpaEntity::getProductId)
                .collect(Collectors.toSet()));
        Map<String, List<CartItem>> itemsByOrderId = itemEntitiesByOrderId.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .map(item -> orderJpaMapper.toDomainOrderItem(
                                        item,
                                        requireProduct(products, item.getProductId())))
                                .toList()));
        Map<String, List<StockAllocation>> allocationsByOrderId =
                orderAllocationRepository.findByOrderIdIn(orderIds).stream()
                        .collect(Collectors.groupingBy(
                                OrderAllocationJpaEntity::getOrderId,
                                Collectors.mapping(orderJpaMapper::toDomain, Collectors.toList())));
        return entities.stream()
                .map(entity -> orderJpaMapper.toDomain(
                        entity,
                        requireBuyer(buyers, entity.getBuyerId()),
                        itemsByOrderId.getOrDefault(entity.getId(), List.of()),
                        allocationsByOrderId.getOrDefault(entity.getId(), List.of()),
                        null))
                .toList();
    }

    public List<CartItem> orderItems(String orderId) {
        return orderItemRepository.findByOrderId(orderId).stream()
                .map(item -> orderJpaMapper.toDomainOrderItem(item, product(item.getProductId())))
                .toList();
    }

    public List<StockAllocation> allocations(String orderId) {
        return orderAllocationRepository.findByOrderId(orderId).stream()
                .map(orderJpaMapper::toDomain)
                .toList();
    }

    public Invoice invoice(InvoiceJpaEntity entity) {
        return invoiceJpaMapper.toDomain(entity, orderWithoutInvoice(entity.getOrderId()));
    }

    public Return returnRequest(ReturnJpaEntity entity) {
        return returnJpaMapper.toDomain(entity, orderWithoutInvoice(entity.getOrderId()));
    }

    public Return returnRequest(String id) {
        return returnRepository.findById(id)
                .map(this::returnRequest)
                .orElseThrow(() -> new IllegalStateException("Return not found: " + id));
    }

    public List<Return> returns(List<ReturnJpaEntity> entities) {
        if (entities.isEmpty()) {
            return List.of();
        }
        Set<String> orderIds = entities.stream()
                .map(ReturnJpaEntity::getOrderId)
                .collect(Collectors.toSet());
        Map<String, Order> ordersById = ordersByIds(orderIds).stream()
                .collect(Collectors.toMap(Order::getId, Function.identity()));
        return entities.stream()
                .map(entity -> returnJpaMapper.toDomain(
                        entity,
                        requireOrder(ordersById, entity.getOrderId())))
                .toList();
    }

    private Map<String, User> usersByIds(Set<String> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserJpaEntity::getId, userMapper::toDomain));
    }

    private Map<String, Buyer> buyersByIds(Set<String> buyerIds) {
        if (buyerIds.isEmpty()) {
            return Map.of();
        }
        Map<String, BuyerJpaEntity> entities = buyerRepository
                .findAllByIdInWithAdditionalAddresses(buyerIds).stream()
                .collect(Collectors.toMap(BuyerJpaEntity::getId, Function.identity()));
        Map<String, User> users = usersByIds(entities.values().stream()
                .map(BuyerJpaEntity::getUserId)
                .collect(Collectors.toSet()));
        return entities.values().stream()
                .collect(Collectors.toMap(
                        BuyerJpaEntity::getId,
                        entity -> buyerMapper.toDomain(entity, requireUser(users, entity.getUserId()))));
    }

    private Map<String, Seller> sellersShallowByIds(Set<String> sellerIds) {
        if (sellerIds.isEmpty()) {
            return Map.of();
        }
        Map<String, SellerJpaEntity> entities = sellerRepository.findAllById(sellerIds).stream()
                .collect(Collectors.toMap(SellerJpaEntity::getId, Function.identity()));
        Map<String, User> users = usersByIds(entities.values().stream()
                .map(SellerJpaEntity::getUserId)
                .collect(Collectors.toSet()));
        return entities.values().stream()
                .collect(Collectors.toMap(
                        SellerJpaEntity::getId,
                        entity -> sellerMapper.toDomain(
                                entity,
                                requireUser(users, entity.getUserId()),
                                List.of())));
    }

    private Map<String, Product> productsByIds(Set<String> productIds) {
        if (productIds.isEmpty()) {
            return Map.of();
        }
        Map<String, ProductJpaEntity> entities = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(ProductJpaEntity::getId, Function.identity()));
        Map<String, Seller> sellers = sellersShallowByIds(entities.values().stream()
                .map(ProductJpaEntity::getSellerId)
                .collect(Collectors.toSet()));
        Map<String, List<ProductVariant>> variantsByProductId =
                productVariantRepository.findByProductIdIn(productIds).stream()
                        .collect(Collectors.groupingBy(
                                ProductVariantJpaEntity::getProductId,
                                Collectors.mapping(productMapper::toDomain, Collectors.toList())));
        return entities.values().stream()
                .collect(Collectors.toMap(
                        ProductJpaEntity::getId,
                        entity -> productMapper.toDomain(
                                entity,
                                requireSeller(sellers, entity.getSellerId()),
                                variantsByProductId.getOrDefault(entity.getId(), List.of()))));
    }

    private Map<String, Warehouse> warehousesByIds(Set<String> warehouseIds) {
        if (warehouseIds.isEmpty()) {
            return Map.of();
        }
        Map<String, WarehouseJpaEntity> entities = warehouseRepository.findAllById(warehouseIds).stream()
                .collect(Collectors.toMap(WarehouseJpaEntity::getId, Function.identity()));
        Map<String, Seller> owners = sellersShallowByIds(entities.values().stream()
                .map(WarehouseJpaEntity::getOwnerSellerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        Map<String, User> users = usersByIds(entities.values().stream()
                .map(WarehouseJpaEntity::getResponsibleUserId)
                .collect(Collectors.toSet()));
        return entities.values().stream()
                .collect(Collectors.toMap(
                        WarehouseJpaEntity::getId,
                        entity -> warehouseMapper.toDomain(
                                entity,
                                entity.getOwnerSellerId() == null
                                        ? null
                                        : requireSeller(owners, entity.getOwnerSellerId()),
                                requireUser(users, entity.getResponsibleUserId()))));
    }

    private static User requireUser(Map<String, User> users, String id) {
        User user = users.get(id);
        if (user == null) {
            throw new UserNotFoundException("User not found: " + id);
        }
        return user;
    }

    private static Buyer requireBuyer(Map<String, Buyer> buyers, String id) {
        Buyer buyer = buyers.get(id);
        if (buyer == null) {
            throw new IllegalStateException("Buyer not found: " + id);
        }
        return buyer;
    }

    private static Seller requireSeller(Map<String, Seller> sellers, String id) {
        Seller seller = sellers.get(id);
        if (seller == null) {
            throw new SellerNotFoundException("Seller not found: " + id);
        }
        return seller;
    }

    private static Product requireProduct(Map<String, Product> products, String id) {
        Product product = products.get(id);
        if (product == null) {
            throw new IllegalStateException("Product not found: " + id);
        }
        return product;
    }

    private static Warehouse requireWarehouse(Map<String, Warehouse> warehouses, String id) {
        Warehouse warehouse = warehouses.get(id);
        if (warehouse == null) {
            throw new IllegalStateException("Warehouse not found: " + id);
        }
        return warehouse;
    }

    private static Order requireOrder(Map<String, Order> orders, String id) {
        Order order = orders.get(id);
        if (order == null) {
            throw new IllegalStateException("Order not found: " + id);
        }
        return order;
    }
}
