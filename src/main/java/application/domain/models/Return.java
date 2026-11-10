package application.domain.models;

import application.domain.enums.OrderStatus;
import application.domain.enums.ReturnStatus;
import application.domain.exceptions.common.InvalidDataException;
import application.domain.exceptions.returns.ReturnAlreadyProcessedException;
import application.domain.exceptions.returns.ReturnNotAllowedException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Return {
    private final String id;
    private final Order order;
    private final String reason;
    private final String productId;
    private final Integer quantity;
    private ReturnStatus status;
    private final Instant requestedAt;

    private Return(String id,
                   Order order,
                   String reason,
                   String productId,
                   Integer quantity,
                   ReturnStatus status,
                   Instant requestedAt) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.order = Objects.requireNonNull(order, "order cannot be null");
        if (reason == null || reason.isBlank()) {
            throw new InvalidDataException("Return reason is required");
        }
        this.reason = reason.trim();
        this.productId = productId;
        this.quantity = quantity;
        this.status = Objects.requireNonNull(status, "status cannot be null");
        this.requestedAt = Objects.requireNonNull(requestedAt, "requestedAt cannot be null");
    }

    public static Return request(Order order, String reason, Instant requestedAt) {
        return request(order, reason, requestedAt, null, null);
    }

    public static Return request(Order order,
                                 String reason,
                                 Instant requestedAt,
                                 String productId,
                                 Integer quantity) {
        Objects.requireNonNull(order, "order cannot be null");
        OrderStatus orderStatus = order.getStatus();
        if (!orderStatus.equals(OrderStatus.DELIVERED) && !orderStatus.equals(OrderStatus.FINALIZED)) {
            throw new ReturnNotAllowedException("Returns are only allowed for delivered orders");
        }
        String normalizedProductId = normalize(productId);
        if (normalizedProductId == null) {
            if (quantity != null) {
                throw new InvalidDataException("Return quantity requires a product");
            }
            return new Return(UUID.randomUUID().toString(), order, reason,
                    null, null, ReturnStatus.REQUESTED, requestedAt);
        }
        if (quantity == null || quantity <= 0) {
            throw new InvalidDataException("Return quantity must be greater than zero");
        }
        int purchasedQuantity = order.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(normalizedProductId))
                .mapToInt(CartItem::getQuantity)
                .sum();
        if (purchasedQuantity == 0) {
            throw new InvalidDataException("Product is not part of the order");
        }
        if (quantity > purchasedQuantity) {
            throw new InvalidDataException("Return quantity exceeds purchased quantity");
        }
        return new Return(UUID.randomUUID().toString(), order, reason,
                normalizedProductId, quantity, ReturnStatus.REQUESTED, requestedAt);
    }

    public static Return reconstitute(String id,
                                      Order order,
                                      String reason,
                                      ReturnStatus status,
                                      Instant requestedAt) {
        return reconstitute(id, order, reason, null, null, status, requestedAt);
    }

    public static Return reconstitute(String id,
                                      Order order,
                                      String reason,
                                      String productId,
                                      Integer quantity,
                                      ReturnStatus status,
                                      Instant requestedAt) {
        return new Return(id, order, reason, productId, quantity, status, requestedAt);
    }

    private static String normalize(String productId) {
        if (productId == null || productId.isBlank()) {
            return null;
        }
        return productId.trim();
    }

    public void approve() {
        if (!status.equals(ReturnStatus.REQUESTED)) {
            throw new ReturnAlreadyProcessedException("Return is already processed");
        }
        status = ReturnStatus.APPROVED;
    }

    public void reject() {
        if (!status.equals(ReturnStatus.REQUESTED)) {
            throw new ReturnAlreadyProcessedException("Return is already processed");
        }
        status = ReturnStatus.REJECTED;
    }

    public void complete() {
        if (!status.equals(ReturnStatus.APPROVED)) {
            throw new ReturnAlreadyProcessedException("Only approved returns can be completed");
        }
        status = ReturnStatus.COMPLETED;
    }

    public String getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    public String getReason() {
        return reason;
    }

    public String getProductId() {
        return productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public ReturnStatus getStatus() {
        return status;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }
}
