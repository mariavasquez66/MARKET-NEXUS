package application.domain.services;

import application.domain.enums.AuditAction;
import application.domain.enums.AuditEntityType;
import application.domain.enums.ProductType;
import application.domain.enums.UserRole;
import application.domain.exceptions.common.UnauthorizedOperationException;
import application.domain.exceptions.product.InvalidProductStateException;
import application.domain.exceptions.product.ProductNotFoundException;
import application.domain.exceptions.product.ProductNotPublishedException;
import application.domain.exceptions.seller.SellerNotFoundException;
import application.domain.exceptions.seller.SellerWithoutWarehouseException;
import application.domain.models.Product;
import application.domain.models.Seller;
import application.domain.models.User;
import application.domain.ports.out.AuditPort;
import application.domain.ports.out.ClockPort;
import application.domain.ports.out.InventoryRepositoryPort;
import application.domain.ports.out.ProductRepositoryPort;
import application.domain.ports.out.SellerRepositoryPort;
import application.domain.valueobjects.Money;
import application.domain.valueobjects.ProductVariant;
import java.util.List;
import java.util.Objects;

public class CatalogService {
    private final ProductRepositoryPort productRepository;
    private final SellerRepositoryPort sellerRepository;
    private final InventoryRepositoryPort inventoryRepository;
    private final AuditPort auditPort;
    private final ClockPort clockPort;
    private final CatalogQueryService queryService;

    public CatalogService(ProductRepositoryPort productRepository,
                          SellerRepositoryPort sellerRepository,
                          InventoryRepositoryPort inventoryRepository,
                          AuditPort auditPort,
                          ClockPort clockPort) {
        this.productRepository = Objects.requireNonNull(productRepository);
        this.sellerRepository = Objects.requireNonNull(sellerRepository);
        this.inventoryRepository = Objects.requireNonNull(inventoryRepository);
        this.auditPort = Objects.requireNonNull(auditPort);
        this.clockPort = Objects.requireNonNull(clockPort);
        this.queryService = new CatalogQueryService(productRepository, sellerRepository);
    }

    public Product registerProduct(User actor,
                                   String name,
                                   String description,
                                   ProductType type,
                                   Money price) {
        ServiceSupport.requireRole(actor, UserRole.SELLER);
        Seller seller = requireSellerFor(actor);
        if (type.equals(ProductType.PHYSICAL) && !seller.hasWarehouses()) {
            throw new SellerWithoutWarehouseException("Physical product requires seller warehouse");
        }
        Product product = Product.create(seller, name, description, type, price);
        Product saved = productRepository.save(product);
        ServiceSupport.audit(auditPort, clockPort, AuditAction.PRODUCT_REGISTERED,
                AuditEntityType.PRODUCT, saved.getId(), actor, "Product registered");
        return saved;
    }

    public Product updateProduct(User actor,
                                 String productId,
                                 String name,
                                 String description,
                                 Money price) {
        Product product = requireOwnedProduct(actor, productId);
        product.changeDetails(name, description);
        product.changePrice(price);
        Product saved = productRepository.save(product);
        ServiceSupport.audit(auditPort, clockPort, AuditAction.PRODUCT_UPDATED,
                AuditEntityType.PRODUCT, saved.getId(), actor, "Product updated");
        return saved;
    }

    public Product addVariant(User actor, String productId, application.domain.enums.VariantType type, String value) {
        Product product = requireOwnedProduct(actor, productId);
        product.addVariant(ProductVariant.of(type, value));
        return productRepository.save(product);
    }

    public Product publishProduct(User actor, String productId) {
        Product product = requireOwnedProduct(actor, productId);
        product.publish();
        if (product.requiresInventory() && !inventoryRepository.existsByProductId(productId)) {
            throw new InvalidProductStateException("Physical product requires initial inventory");
        }
        Product saved = productRepository.save(product);
        ServiceSupport.audit(auditPort, clockPort, AuditAction.PRODUCT_PUBLISHED,
                AuditEntityType.PRODUCT, saved.getId(), actor, "Product published");
        return saved;
    }

    public Product suspendProduct(User actor, String productId) {
        Product product = requireOwnedProduct(actor, productId);
        product.suspend();
        Product saved = productRepository.save(product);
        ServiceSupport.audit(auditPort, clockPort, AuditAction.PRODUCT_SUSPENDED,
                AuditEntityType.PRODUCT, saved.getId(), actor, "Product suspended");
        return saved;
    }

    public Product discontinueProduct(User actor, String productId) {
        Product product = requireOwnedProduct(actor, productId);
        product.discontinue();
        Product saved = productRepository.save(product);
        ServiceSupport.audit(auditPort, clockPort, AuditAction.PRODUCT_DISCONTINUED,
                AuditEntityType.PRODUCT, saved.getId(), actor, "Product discontinued");
        return saved;
    }

    public List<Product> listPublishedProducts() {
        return queryService.listPublishedProducts();
    }

    public Product getPublishedProduct(String productId) {
        return queryService.getPublishedProduct(productId);
    }

    public List<Product> listMyProducts(User actor) {
        return queryService.listMyProducts(actor);
    }

    public List<Product> listBySeller(User actor, String sellerId) {
        return queryService.listBySeller(actor, sellerId);
    }

    public Product getProductForSeller(User actor, String productId) {
        return queryService.getProductForSeller(actor, productId);
    }

    private Product requireOwnedProduct(User actor, String productId) {
        ServiceSupport.requireRole(actor, UserRole.SELLER);
        Product product = requireProduct(productId);
        if (!product.getSeller().getUser().getId().equals(actor.getId())) {
            throw new UnauthorizedOperationException("Cannot manage another seller product");
        }
        return product;
    }

    private Product requireProduct(String productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found: " + productId));
    }

    private Seller requireSellerFor(User actor) {
        return sellerRepository.findByUserId(actor.getId())
                .orElseThrow(() -> new SellerNotFoundException("Seller not found for current user"));
    }
}
