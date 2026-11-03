package application.domain.models;

import application.domain.exceptions.cart.EmptyCartException;
import application.domain.exceptions.cart.InvalidQuantityException;
import application.domain.exceptions.cart.ProductNotAvailableException;
import application.domain.valueobjects.Money;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public class Cart {
    private final String id;
    private final Buyer buyer;
    private final List<CartItem> items;

    private Cart(String id, Buyer buyer, List<CartItem> items) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.buyer = Objects.requireNonNull(buyer, "buyer cannot be null");
        this.items = new ArrayList<>(Objects.requireNonNull(items));
    }

    public static Cart create(Buyer buyer) {
        Objects.requireNonNull(buyer, "buyer cannot be null");
        return new Cart(UUID.randomUUID().toString(), buyer, List.of());
    }

    public static Cart reconstitute(String id, Buyer buyer, List<CartItem> items) {
        return new Cart(id, buyer, items);
    }

    public void addItem(Product product, int quantity) {
        buyer.assertCanPurchase();
        if (product == null || !product.isPurchasable()) {
            throw new ProductNotAvailableException("Product is not available");
        }
        if (quantity <= 0) {
            throw new InvalidQuantityException("Quantity must be greater than zero");
        }
        Optional<CartItem> existing = find(product.getId());
        if (existing.isPresent()) {
            CartItem item = existing.get();
            item.changeQuantity(item.getQuantity() + quantity);
            return;
        }
        items.add(CartItem.create(product, quantity));
    }

    public void updateQuantity(String productId, int quantity) {
        CartItem item = find(productId)
                .orElseThrow(() -> new ProductNotAvailableException("Product is not in the cart"));
        item.changeQuantity(quantity);
    }

    public void removeItem(String productId) {
        CartItem item = find(productId)
                .orElseThrow(() -> new ProductNotAvailableException("Product is not in the cart"));
        items.remove(item);
    }

    public void clear() {
        items.clear();
    }

    public Money total() {
        Money total = Money.cop(BigDecimal.ZERO);
        for (CartItem item : items) {
            total = total.add(item.subtotal());
        }
        return total;
    }

    public void assertNotEmpty() {
        if (items.isEmpty()) {
            throw new EmptyCartException("Cart is empty");
        }
    }

    private Optional<CartItem> find(String productId) {
        if (productId == null) {
            return Optional.empty();
        }
        return items.stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst();
    }

    public String getId() {
        return id;
    }

    public Buyer getBuyer() {
        return buyer;
    }

    public List<CartItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}
