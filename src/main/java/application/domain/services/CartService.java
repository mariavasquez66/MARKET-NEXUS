package application.domain.services;

import application.domain.enums.UserRole;
import application.domain.exceptions.cart.CartNotFoundException;
import application.domain.exceptions.cart.InvalidQuantityException;
import application.domain.exceptions.cart.ProductNotAvailableException;
import application.domain.exceptions.common.InvalidDataException;
import application.domain.exceptions.product.ProductNotFoundException;
import application.domain.models.Buyer;
import application.domain.models.Cart;
import application.domain.models.Product;
import application.domain.models.User;
import application.domain.ports.out.BuyerRepositoryPort;
import application.domain.ports.out.CartRepositoryPort;
import application.domain.ports.out.ProductRepositoryPort;
import java.util.Objects;

public class CartService {
    private final CartRepositoryPort cartRepository;
    private final BuyerRepositoryPort buyerRepository;
    private final ProductRepositoryPort productRepository;

    public CartService(CartRepositoryPort cartRepository,
                       BuyerRepositoryPort buyerRepository,
                       ProductRepositoryPort productRepository) {
        this.cartRepository = Objects.requireNonNull(cartRepository);
        this.buyerRepository = Objects.requireNonNull(buyerRepository);
        this.productRepository = Objects.requireNonNull(productRepository);
    }

    public Cart getCart(User actor) {
        Buyer buyer = requireBuyer(actor);
        return cartRepository.findByBuyerId(buyer.getId())
                .orElseGet(() -> cartRepository.save(Cart.create(buyer)));
    }

    public Cart addItem(User actor, String productId, int quantity) {
        Buyer buyer = requireBuyer(actor);
        if (quantity <= 0) {
            throw new InvalidQuantityException("Quantity must be greater than zero");
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException("Product not found: " + productId));
        if (!product.isPurchasable()) {
            throw new ProductNotAvailableException("Product is not available");
        }
        buyer.assertCanPurchase();
        Cart cart = cartRepository.findByBuyerId(buyer.getId())
                .orElseGet(() -> Cart.create(buyer));
        cart.addItem(product, quantity);
        return cartRepository.save(cart);
    }

    public Cart updateQuantity(User actor, String productId, int quantity) {
        Buyer buyer = requireBuyer(actor);
        if (quantity <= 0) {
            throw new InvalidQuantityException("Quantity must be greater than zero");
        }
        Cart cart = requireCart(buyer);
        ensureItemExists(cart, productId);
        cart.updateQuantity(productId, quantity);
        return cartRepository.save(cart);
    }

    public Cart removeItem(User actor, String productId) {
        Buyer buyer = requireBuyer(actor);
        Cart cart = requireCart(buyer);
        ensureItemExists(cart, productId);
        cart.removeItem(productId);
        return cartRepository.save(cart);
    }

    public void clearCart(User actor) {
        Buyer buyer = requireBuyer(actor);
        Cart cart = requireCart(buyer);
        cart.clear();
        cartRepository.save(cart);
    }

    private Buyer requireBuyer(User actor) {
        ServiceSupport.requireRole(actor, UserRole.BUYER);
        return buyerRepository.findByUserId(actor.getId())
                .orElseThrow(() -> new application.domain.exceptions.buyer.BuyerNotFoundException(
                        "Buyer not found for current user"));
    }

    private Cart requireCart(Buyer buyer) {
        return cartRepository.findByBuyerId(buyer.getId())
                .orElseThrow(() -> new CartNotFoundException("Cart not found"));
    }

    private void ensureItemExists(Cart cart, String productId) {
        boolean exists = cart.getItems().stream()
                .anyMatch(item -> item.getProduct().getId().equals(productId));
        if (!exists) {
            throw new InvalidDataException("Product is not in the cart");
        }
    }
}
