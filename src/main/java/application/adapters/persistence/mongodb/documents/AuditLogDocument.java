package application.adapters.persistence.mongodb.documents;

import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@NoArgsConstructor
@Document(collection = "audit_logs")
public class AuditLogDocument {
    @Id
    private String id;
    private String actionCode;
    private String entityTypeCode;
    private String entityId;
    private String performedBy;
    private String roleCode;
    private Instant occurredAt;
    private String details;
}
