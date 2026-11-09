package application.domain.services;

import application.domain.enums.UserRole;
import application.domain.exceptions.common.UnauthorizedOperationException;
import application.domain.exceptions.product.ProductNotFoundException;
import application.domain.exceptions.product.ProductNotPublishedException;
import application.domain.exceptions.seller.SellerNotFoundException;
import application.domain.models.Product;
import application.domain.models.Seller;
import application.domain.models.User;
import application.domain.ports.out.ProductRepositoryPort;
import application.domain.ports.out.SellerRepositoryPort;
import java.util.List;
import java.util.Objects;

public class CatalogQueryService {
    private final ProductRepositoryPort productRepository;
    private final SellerRepositoryPort sellerRepository;

    public CatalogQueryService(ProductRepositoryPort productRepository,
                               SellerRepositoryPort sellerRepository) {
        this.productRepository = Objects.requireNonNull(productRepository);
        this.sellerRepository = Objects.requireNonNull(sellerRepository);
    }

    public List<Product> listPublishedProducts() {
        return productRepository.findPublished();
    }

    public Product getPublishedProduct(String productId) {
        Product product = requireProduct(productId);
        if (!product.isPurchasable()) {
            throw new ProductNotPublishedException("Product is not published: " + productId);
        }
        return product;
    }

    public List<Product> listMyProducts(User actor) {
        ServiceSupport.requireRole(actor, UserRole.SELLER);
        Seller seller = sellerRepository.findByUserId(actor.getId())
                .orElseThrow(() -> new SellerNotFoundException("Seller not found for current user"));
        return productRepository.findBySellerId(seller.getId());
    }

    public List<Product> listBySeller(User actor, String sellerId) {
        ServiceSupport.requireAnyRole(actor, UserRole.SELLER, UserRole.ADMIN);
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new SellerNotFoundException("Seller not found: " + sellerId));
        if (!actor.hasRole(UserRole.ADMIN) && !seller.getUser().getId().equals(actor.getId())) {
            throw new UnauthorizedOperationException("Cannot access another seller catalog");
        }
        return productRepository.findBySellerId(sellerId);
    }

    public Product getProductForSeller(User actor, String productId) {
        ServiceSupport.requireAnyRole(actor, UserRole.SELLER, UserRole.ADMIN);
        Product product = requireProduct(productId);
        if (actor.hasRole(UserRole.ADMIN)) {
            return product;
        }
        if (!product.getSeller().getUser().getId().equals(actor.getId())) {
            throw new UnauthorizedOperationException("Cannot access another seller product");
        }
        return product;
    }

    private Product requireProduct(String productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found: " + productId));
    }
}
