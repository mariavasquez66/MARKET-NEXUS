package application.domain.services;

import application.domain.enums.AuditAction;
import application.domain.enums.AuditEntityType;
import application.domain.enums.UserRole;
import application.domain.exceptions.common.InsufficientPermissionException;
import application.domain.models.AuditLog;
import application.domain.models.User;
import application.domain.ports.out.AuditPort;
import application.domain.ports.out.ClockPort;
import java.util.Objects;

final class ServiceSupport {

    private ServiceSupport() {
    }

    static User requireRole(User actor, UserRole role) {
        Objects.requireNonNull(actor, "actor cannot be null");
        actor.assertCanOperate();
        if (!actor.hasRole(role)) {
            throw new InsufficientPermissionException("Operation requires role " + role.getCode());
        }
        return actor;
    }

    static User requireAnyRole(User actor, UserRole... roles) {
        Objects.requireNonNull(actor, "actor cannot be null");
        actor.assertCanOperate();
        for (UserRole role : roles) {
            if (actor.hasRole(role)) {
                return actor;
            }
        }
        throw new InsufficientPermissionException("Operation requires an authorized role");
    }

    static void audit(AuditPort auditPort,
                      ClockPort clockPort,
                      AuditAction action,
                      AuditEntityType entityType,
                      String entityId,
                      User actor,
                      String details) {
        auditPort.record(AuditLog.create(action, entityType, entityId, actor,
                clockPort.now(), details));
    }
}
