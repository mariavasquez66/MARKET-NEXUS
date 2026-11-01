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
    private ReturnStatus status;
    private final Instant requestedAt;

    private Return(String id, Order order, String reason, ReturnStatus status, Instant requestedAt) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.order = Objects.requireNonNull(order, "order cannot be null");
        if (reason == null || reason.isBlank()) {
            throw new InvalidDataException("Return reason is required");
        }
        this.reason = reason.trim();
        this.status = Objects.requireNonNull(status, "status cannot be null");
        this.requestedAt = Objects.requireNonNull(requestedAt, "requestedAt cannot be null");
    }

    public static Return request(Order order, String reason, Instant requestedAt) {
        Objects.requireNonNull(order, "order cannot be null");
        OrderStatus orderStatus = order.getStatus();
        if (!orderStatus.equals(OrderStatus.DELIVERED) && !orderStatus.equals(OrderStatus.FINALIZED)) {
            throw new ReturnNotAllowedException("Returns are only allowed for delivered orders");
        }
        return new Return(UUID.randomUUID().toString(), order, reason,
                ReturnStatus.REQUESTED, requestedAt);
    }

    public static Return reconstitute(String id,
                                      Order order,
                                      String reason,
                                      ReturnStatus status,
                                      Instant requestedAt) {
        return new Return(id, order, reason, status, requestedAt);
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

    public ReturnStatus getStatus() {
        return status;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }
}
