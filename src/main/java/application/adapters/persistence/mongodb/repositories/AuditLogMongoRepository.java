package application.adapters.persistence.mongodb.repositories;

import application.adapters.persistence.mongodb.documents.AuditLogDocument;
import java.time.Instant;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AuditLogMongoRepository extends MongoRepository<AuditLogDocument, String> {

    List<AuditLogDocument> findByActionCode(String actionCode);

    List<AuditLogDocument> findByActionCodeAndOccurredAtBetween(
            String actionCode, Instant from, Instant to);

    List<AuditLogDocument> findByOccurredAtBetween(Instant from, Instant to);

    List<AuditLogDocument> findAllByOrderByOccurredAtDesc();
}
