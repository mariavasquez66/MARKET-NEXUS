package application.adapters.rest.dto.response;

import java.util.List;

public record CartResponse(
        String id,
        String buyerId,
        List<CartItemResponse> items,
        MoneyResponse total) {
}
