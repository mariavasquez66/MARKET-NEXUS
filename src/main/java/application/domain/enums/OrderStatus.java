package application.domain.enums;

import application.domain.exceptions.common.InvalidDataException;
import java.util.HashMap;
import java.util.Map;

public class OrderStatus extends DomainCatalog {
    public static final OrderStatus CART = new OrderStatus("CART", "Cart");
    public static final OrderStatus PENDING_PAYMENT = new OrderStatus("PENDING_PAYMENT", "Pending Payment");
    public static final OrderStatus PAID = new OrderStatus("PAID", "Paid");
    public static final OrderStatus DISPATCHED = new OrderStatus("DISPATCHED", "Dispatched");
    public static final OrderStatus DELIVERED = new OrderStatus("DELIVERED", "Delivered");
    public static final OrderStatus FINALIZED = new OrderStatus("FINALIZED", "Finalized");
    public static final OrderStatus CANCELLED = new OrderStatus("CANCELLED", "Cancelled");

    private static final Map<String, OrderStatus> VALUES = new HashMap<>();

    static {
        VALUES.put("CART", CART);
        VALUES.put("PENDING_PAYMENT", PENDING_PAYMENT);
        VALUES.put("PAID", PAID);
        VALUES.put("DISPATCHED", DISPATCHED);
        VALUES.put("DELIVERED", DELIVERED);
        VALUES.put("FINALIZED", FINALIZED);
        VALUES.put("CANCELLED", CANCELLED);
    }

    private OrderStatus(String code, String description) {
        super(code, description);
    }

    public static OrderStatus fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new InvalidDataException("OrderStatus code cannot be null or blank");
        }
        OrderStatus status = VALUES.get(code.toUpperCase());
        if (status == null) {
            throw new InvalidDataException("Invalid OrderStatus code: " + code);
        }
        return status;
    }
}
