package application.domain.models.reports;

import application.domain.exceptions.common.InvalidDataException;

public record OrderSummary(int totalOrders,
                           int pendingPayment,
                           int paid,
                           int dispatched,
                           int delivered,
                           int finalized,
                           int cancelled) {
    public OrderSummary {
        if (totalOrders < 0 || pendingPayment < 0 || paid < 0 || dispatched < 0
                || delivered < 0 || finalized < 0 || cancelled < 0) {
            throw new InvalidDataException("Order summary counts cannot be negative");
        }
    }
}
