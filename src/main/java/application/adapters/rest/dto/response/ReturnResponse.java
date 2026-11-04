package application.adapters.rest.dto.response;

import java.time.Instant;

public record ReturnResponse(
        String id,
        String orderId,
        String reason,
        String status,
        Instant requestedAt) {
}
