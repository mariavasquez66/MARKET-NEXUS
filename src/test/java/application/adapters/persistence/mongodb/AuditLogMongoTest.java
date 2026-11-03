package application.adapters.persistence.mongodb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import application.adapters.persistence.mongodb.adapters.AuditLogMongoAdapter;
import application.adapters.persistence.mongodb.documents.AuditLogDocument;
import application.adapters.persistence.mongodb.mappers.AuditLogMongoMapper;
import application.adapters.persistence.mongodb.repositories.AuditLogMongoRepository;
import application.domain.enums.AuditAction;
import application.domain.enums.AuditEntityType;
import application.domain.enums.UserRole;
import application.domain.models.AuditLog;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditLogMongoTest {

    private static final Instant OCCURRED_AT = Instant.parse("2026-05-10T12:30:45Z");

    @Mock
    private AuditLogMongoRepository repository;

    private AuditLogMongoMapper mapper;
    private AuditLogMongoAdapter adapter;

    @BeforeEach
    void setUp() {
        mapper = new AuditLogMongoMapper();
        adapter = new AuditLogMongoAdapter(repository, mapper);
    }

    @Test
    void mapperRoundTripPreservesEveryPersistedField() {
        AuditLog auditLog = auditLog("audit-1", AuditAction.ORDER_CREATED, OCCURRED_AT);

        AuditLogDocument document = mapper.toDocument(auditLog);

        assertThat(document.getId()).isEqualTo("audit-1");
        assertThat(document.getActionCode()).isEqualTo("ORDER_CREATED");
        assertThat(document.getEntityTypeCode()).isEqualTo("ORDER");
        assertThat(document.getEntityId()).isEqualTo("order-audit-1");
        assertThat(document.getPerformedBy()).isEqualTo("user-audit-1");
        assertThat(document.getRoleCode()).isEqualTo("ADMIN");
        assertThat(document.getOccurredAt()).isEqualTo(OCCURRED_AT);
        assertThat(document.getDetails()).isEqualTo("details-audit-1");

        AuditLog roundTrip = mapper.toDomain(document);

        assertThat(roundTrip.getId()).isEqualTo(auditLog.getId());
        assertThat(roundTrip.getAction()).isEqualTo(AuditAction.ORDER_CREATED);
        assertThat(roundTrip.getEntityType()).isEqualTo(AuditEntityType.ORDER);
        assertThat(roundTrip.getEntityId()).isEqualTo(auditLog.getEntityId());
        assertThat(roundTrip.getPerformedBy()).isEqualTo(auditLog.getPerformedBy());
        assertThat(roundTrip.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(roundTrip.getOccurredAt()).isEqualTo(OCCURRED_AT);
        assertThat(roundTrip.getDetails()).isEqualTo(auditLog.getDetails());
    }

    @Test
    void recordSavesMappedDocument() {
        AuditLog auditLog = auditLog("audit-record", AuditAction.PRODUCT_PUBLISHED, OCCURRED_AT);

        adapter.record(auditLog);

        ArgumentCaptor<AuditLogDocument> captor = ArgumentCaptor.forClass(AuditLogDocument.class);
        verify(repository).save(captor.capture());
        AuditLogDocument saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo("audit-record");
        assertThat(saved.getActionCode()).isEqualTo("PRODUCT_PUBLISHED");
        assertThat(saved.getEntityTypeCode()).isEqualTo("ORDER");
        assertThat(saved.getEntityId()).isEqualTo("order-audit-record");
        assertThat(saved.getPerformedBy()).isEqualTo("user-audit-record");
        assertThat(saved.getRoleCode()).isEqualTo("ADMIN");
        assertThat(saved.getOccurredAt()).isEqualTo(OCCURRED_AT);
        assertThat(saved.getDetails()).isEqualTo("details-audit-record");
    }

    @Test
    void findAllMapsDocumentsReturnedByRepository() {
        AuditLogDocument first = mapper.toDocument(
                auditLog("audit-1", AuditAction.ORDER_CREATED, OCCURRED_AT));
        AuditLogDocument second = mapper.toDocument(
                auditLog("audit-2", AuditAction.ORDER_STATUS_CHANGED, OCCURRED_AT.plusSeconds(60)));
        when(repository.findAllByOrderByOccurredAtDesc()).thenReturn(List.of(first, second));

        List<AuditLog> result = adapter.findAll();

        assertThat(result).extracting(AuditLog::getId)
                .containsExactly("audit-1", "audit-2");
        assertThat(result).extracting(AuditLog::getAction)
                .containsExactly(AuditAction.ORDER_CREATED, AuditAction.ORDER_STATUS_CHANGED);
        verify(repository).findAllByOrderByOccurredAtDesc();
    }

    @Test
    void findWithActionOnlyUsesFindByActionCode() {
        AuditAction action = AuditAction.INVOICE_ISSUED;
        AuditLogDocument document = mapper.toDocument(
                auditLog("audit-invoice", action, OCCURRED_AT));
        when(repository.findByActionCode(action.getCode())).thenReturn(List.of(document));

        List<AuditLog> result = adapter.find(action, null, null);

        assertThat(result).extracting(AuditLog::getId).containsExactly("audit-invoice");
        verify(repository).findByActionCode(action.getCode());
    }

    @Test
    void findWithDateRangeOnlyUsesFindByOccurredAtBetween() {
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = LocalDate.of(2026, 5, 31);
        Instant fromInstant = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant = to.atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC);
        AuditLogDocument document = mapper.toDocument(
                auditLog("audit-range", AuditAction.RETURN_REQUESTED, OCCURRED_AT));
        when(repository.findByOccurredAtBetween(fromInstant, toInstant))
                .thenReturn(List.of(document));

        List<AuditLog> result = adapter.find(null, from, to);

        assertThat(result).extracting(AuditLog::getId).containsExactly("audit-range");
        verify(repository).findByOccurredAtBetween(fromInstant, toInstant);
    }

    @Test
    void findWithActionAndDateRangeUsesCombinedRepositoryQuery() {
        AuditAction action = AuditAction.SHIPMENT_STARTED;
        LocalDate from = LocalDate.of(2026, 5, 1);
        LocalDate to = LocalDate.of(2026, 5, 31);
        Instant fromInstant = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant = to.atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC);
        AuditLogDocument document = mapper.toDocument(
                auditLog("audit-combined", action, OCCURRED_AT));
        when(repository.findByActionCodeAndOccurredAtBetween(
                action.getCode(), fromInstant, toInstant)).thenReturn(List.of(document));

        List<AuditLog> result = adapter.find(action, from, to);

        assertThat(result).extracting(AuditLog::getId).containsExactly("audit-combined");
        verify(repository).findByActionCodeAndOccurredAtBetween(
                action.getCode(), fromInstant, toInstant);
    }

    @Test
    void findWithNoFiltersUsesOrderedFindAll() {
        AuditLogDocument document = mapper.toDocument(
                auditLog("audit-ordered", AuditAction.REFUND_PROCESSED, OCCURRED_AT));
        when(repository.findAllByOrderByOccurredAtDesc()).thenReturn(List.of(document));

        List<AuditLog> result = adapter.find(null, null, null);

        assertThat(result).extracting(AuditLog::getId).containsExactly("audit-ordered");
        verify(repository).findAllByOrderByOccurredAtDesc();
        verify(repository, never()).findAll();
    }

    private static AuditLog auditLog(String id, AuditAction action, Instant occurredAt) {
        return AuditLog.reconstitute(id, action, AuditEntityType.ORDER, "order-" + id,
                "user-" + id, UserRole.ADMIN, occurredAt, "details-" + id);
    }
}
