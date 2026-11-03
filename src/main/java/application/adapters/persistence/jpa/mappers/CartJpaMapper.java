package application.adapters.persistence.jpa.mappers;

import application.adapters.persistence.jpa.entities.CartItemJpaEntity;
import application.adapters.persistence.jpa.entities.CartJpaEntity;
import application.domain.models.Buyer;
import application.domain.models.Cart;
import application.domain.models.CartItem;
import application.domain.models.Product;
import application.domain.valueobjects.Money;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CartJpaMapper {

    public CartJpaEntity toEntity(Cart cart) {
        CartJpaEntity entity = new CartJpaEntity();
        entity.setId(cart.getId());
        entity.setBuyerId(cart.getBuyer().getId());
        return entity;
    }

    public Cart toDomain(CartJpaEntity entity, Buyer buyer, List<CartItem> items) {
        return Cart.reconstitute(entity.getId(), buyer, items);
    }

    public CartItemJpaEntity toEntity(CartItem item, String cartId) {
        CartItemJpaEntity entity = new CartItemJpaEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setCartId(cartId);
        entity.setProductId(item.getProduct().getId());
        entity.setQuantity(item.getQuantity());
        entity.setUnitPriceAmount(item.getUnitPrice().getAmount());
        entity.setUnitPriceCurrency(item.getUnitPrice().getCurrency().getCurrencyCode());
        return entity;
    }

    public CartItem toDomain(CartItemJpaEntity entity, Product product) {
        Money unitPrice = Money.of(
                entity.getUnitPriceAmount(),
                Currency.getInstance(entity.getUnitPriceCurrency()));
        return CartItem.reconstitute(product, entity.getQuantity(), unitPrice);
    }
}
