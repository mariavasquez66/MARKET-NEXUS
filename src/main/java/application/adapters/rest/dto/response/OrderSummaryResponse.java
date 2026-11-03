package application.adapters.rest.dto.response;

import java.util.Map;

public record OrderSummaryResponse(
        int totalOrders,
        Map<String, Integer> ordersByStatus,
        int pendingPayment,
        int paid,
        int dispatched,
        int delivered,
        int finalized,
        int cancelled) {
}
