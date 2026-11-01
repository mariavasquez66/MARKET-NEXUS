package application.domain.models;

import application.domain.enums.AuditAction;
import application.domain.enums.AuditEntityType;
import application.domain.enums.UserRole;
import application.domain.exceptions.common.InvalidDataException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class AuditLog {
    private final String id;
    private final AuditAction action;
    private final AuditEntityType entityType;
    private final String entityId;
    private final String performedBy;
    private final UserRole role;
    private final Instant occurredAt;
    private final String details;

    private AuditLog(String id,
                     AuditAction action,
                     AuditEntityType entityType,
                     String entityId,
                     String performedBy,
                     UserRole role,
                     Instant occurredAt,
                     String details) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.action = Objects.requireNonNull(action, "action cannot be null");
        this.entityType = Objects.requireNonNull(entityType, "entityType cannot be null");
        if (entityId == null || entityId.isBlank()) {
            throw new InvalidDataException("Audit entity id is required");
        }
        this.entityId = entityId;
        this.performedBy = Objects.requireNonNull(performedBy, "performedBy cannot be null");
        this.role = Objects.requireNonNull(role, "role cannot be null");
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt cannot be null");
        this.details = details == null ? "" : details;
    }

    public static AuditLog create(AuditAction action,
                                  AuditEntityType entityType,
                                  String entityId,
                                  User performedBy,
                                  Instant occurredAt,
                                  String details) {
        Objects.requireNonNull(performedBy, "performedBy cannot be null");
        return new AuditLog(UUID.randomUUID().toString(), action, entityType, entityId,
                performedBy.getId(), performedBy.getRole(), occurredAt, details);
    }

    public static AuditLog reconstitute(String id,
                                        AuditAction action,
                                        AuditEntityType entityType,
                                        String entityId,
                                        String performedBy,
                                        UserRole role,
                                        Instant occurredAt,
                                        String details) {
        return new AuditLog(id, action, entityType, entityId, performedBy, role, occurredAt, details);
    }

    public String getId() {
        return id;
    }

    public AuditAction getAction() {
        return action;
    }

    public AuditEntityType getEntityType() {
        return entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public UserRole getRole() {
        return role;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getDetails() {
        return details;
    }
}
