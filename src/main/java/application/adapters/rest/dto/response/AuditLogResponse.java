package application.adapters.rest.dto.response;

import java.time.Instant;

public record AuditLogResponse(
        String id,
        String action,
        String entityType,
        String entityId,
        String performedBy,
        String role,
        Instant occurredAt,
        String details) {
}
