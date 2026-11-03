package application.adapters.persistence.mongodb.mappers;

import application.adapters.persistence.mongodb.documents.AuditLogDocument;
import application.domain.enums.AuditAction;
import application.domain.enums.AuditEntityType;
import application.domain.enums.UserRole;
import application.domain.models.AuditLog;
import org.springframework.stereotype.Component;

@Component
public class AuditLogMongoMapper {

    public AuditLogDocument toDocument(AuditLog auditLog) {
        if (auditLog == null) {
            return null;
        }
        AuditLogDocument document = new AuditLogDocument();
        document.setId(auditLog.getId());
        document.setActionCode(auditLog.getAction() == null ? null : auditLog.getAction().getCode());
        document.setEntityTypeCode(
                auditLog.getEntityType() == null ? null : auditLog.getEntityType().getCode());
        document.setEntityId(auditLog.getEntityId());
        document.setPerformedBy(auditLog.getPerformedBy());
        document.setRoleCode(auditLog.getRole() == null ? null : auditLog.getRole().getCode());
        document.setOccurredAt(auditLog.getOccurredAt());
        document.setDetails(auditLog.getDetails());
        return document;
    }

    public AuditLog toDomain(AuditLogDocument document) {
        if (document == null) {
            return null;
        }
        return AuditLog.reconstitute(
                document.getId(),
                AuditAction.fromCode(document.getActionCode()),
                AuditEntityType.fromCode(document.getEntityTypeCode()),
                document.getEntityId(),
                document.getPerformedBy(),
                UserRole.fromCode(document.getRoleCode()),
                document.getOccurredAt(),
                document.getDetails());
    }
}
