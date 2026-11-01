package application.domain.ports.out;

import application.domain.enums.AuditAction;
import application.domain.models.AuditLog;
import java.time.LocalDate;
import java.util.List;

public interface AuditPort {
    void record(AuditLog auditLog);

    List<AuditLog> find(AuditAction action, LocalDate from, LocalDate to);

    List<AuditLog> findAll();
}
