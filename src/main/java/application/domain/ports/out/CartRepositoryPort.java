package application.domain.ports.out;

import application.domain.models.Cart;
import java.util.Optional;

public interface CartRepositoryPort {
    Cart save(Cart cart);

    Optional<Cart> findByBuyerId(String buyerId);

    void deleteByBuyerId(String buyerId);
}
