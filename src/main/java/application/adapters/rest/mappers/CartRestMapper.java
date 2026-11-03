package application.adapters.rest.mappers;

import application.adapters.rest.dto.response.CartItemResponse;
import application.adapters.rest.dto.response.CartResponse;
import application.domain.models.Cart;
import application.domain.models.CartItem;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CartRestMapper extends RestMapperSupport {

    public CartResponse toResponse(Cart cart) {
        List<CartItemResponse> items = cart.getItems().stream()
                .map(this::toResponse)
                .toList();
        return new CartResponse(
                cart.getId(),
                cart.getBuyer().getId(),
                List.copyOf(items),
                toMoneyResponse(cart.total()));
    }

    public CartItemResponse toResponse(CartItem item) {
        return new CartItemResponse(
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getQuantity(),
                toMoneyResponse(item.getUnitPrice()),
                toMoneyResponse(item.subtotal()));
    }
}
