package application.adapters.persistence.jpa.adapters;

import application.adapters.persistence.jpa.entities.BuyerJpaEntity;
import application.adapters.persistence.jpa.entities.InventoryJpaEntity;
import application.adapters.persistence.jpa.entities.InvoiceJpaEntity;
import application.adapters.persistence.jpa.entities.OrderJpaEntity;
import application.adapters.persistence.jpa.entities.ProductJpaEntity;
import application.adapters.persistence.jpa.entities.ReturnJpaEntity;
import application.adapters.persistence.jpa.entities.SellerJpaEntity;
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
import java.util.List;
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
}
