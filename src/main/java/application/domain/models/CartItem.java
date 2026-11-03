package application.domain.models;

import application.domain.exceptions.cart.InvalidQuantityException;
import application.domain.valueobjects.Money;
import java.util.Objects;

public class CartItem {
    private final Product product;
    private int quantity;
    private final Money unitPrice;

    private CartItem(Product product, int quantity, Money unitPrice) {
        this.product = Objects.requireNonNull(product, "product cannot be null");
        if (quantity <= 0) {
            throw new InvalidQuantityException("Cart item quantity must be greater than zero");
        }
        this.quantity = quantity;
        this.unitPrice = Objects.requireNonNull(unitPrice, "unitPrice cannot be null");
    }

    public static CartItem create(Product product, int quantity) {
        Objects.requireNonNull(product, "product cannot be null");
        return new CartItem(product, quantity, product.getPrice());
    }

    public static CartItem reconstitute(Product product, int quantity, Money unitPrice) {
        return new CartItem(product, quantity, unitPrice);
    }

    public void changeQuantity(int quantity) {
        if (quantity <= 0) {
            throw new InvalidQuantityException("Cart item quantity must be greater than zero");
        }
        this.quantity = quantity;
    }

    public Money subtotal() {
        return unitPrice.multiply(quantity);
    }

    public Product getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity;
    }

    public Money getUnitPrice() {
        return unitPrice;
    }
}
