package application.adapters.persistence.mongodb.adapters;

import application.adapters.persistence.mongodb.documents.AuditLogDocument;
import application.adapters.persistence.mongodb.mappers.AuditLogMongoMapper;
import application.adapters.persistence.mongodb.repositories.AuditLogMongoRepository;
import application.domain.enums.AuditAction;
import application.domain.models.AuditLog;
import application.domain.ports.out.AuditPort;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuditLogMongoAdapter implements AuditPort {

    private static final Instant MIN_INSTANT = Instant.EPOCH;
    private static final Instant MAX_INSTANT = Instant.parse("9999-12-31T23:59:59.999Z");

    private final AuditLogMongoRepository repository;
    private final AuditLogMongoMapper mapper;

    @Override
    public void record(AuditLog auditLog) {
        repository.save(mapper.toDocument(auditLog));
    }

    @Override
    public List<AuditLog> find(AuditAction action, LocalDate from, LocalDate to) {
        if (action == null && from == null && to == null) {
            return findAll();
        }
        if (action != null && from == null && to == null) {
            return map(repository.findByActionCode(action.getCode()));
        }
        Instant fromInstant = toInstantStart(from);
        Instant toInstant = toInstantEnd(to);
        if (action == null) {
            return map(repository.findByOccurredAtBetween(fromInstant, toInstant));
        }
        return map(repository.findByActionCodeAndOccurredAtBetween(
                action.getCode(), fromInstant, toInstant));
    }

    @Override
    public List<AuditLog> findAll() {
        return map(repository.findAllByOrderByOccurredAtDesc());
    }

    private List<AuditLog> map(List<AuditLogDocument> documents) {
        return documents.stream().map(mapper::toDomain).toList();
    }

    private Instant toInstantStart(LocalDate date) {
        return date == null ? MIN_INSTANT : date.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private Instant toInstantEnd(LocalDate date) {
        return date == null ? MAX_INSTANT : date.atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC);
    }
}
