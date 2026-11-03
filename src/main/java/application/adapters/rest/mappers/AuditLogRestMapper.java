package application.adapters.rest.mappers;

import application.adapters.rest.dto.response.AuditLogResponse;
import application.domain.models.AuditLog;
import org.springframework.stereotype.Component;

@Component
public class AuditLogRestMapper extends RestMapperSupport {

    public AuditLogResponse toResponse(AuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getAction().getCode(),
                auditLog.getEntityType().getCode(),
                auditLog.getEntityId(),
                auditLog.getPerformedBy(),
                auditLog.getRole().getCode(),
                auditLog.getOccurredAt(),
                auditLog.getDetails());
    }
}
