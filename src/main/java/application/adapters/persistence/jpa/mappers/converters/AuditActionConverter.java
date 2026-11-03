package application.adapters.persistence.jpa.mappers.converters;

import application.domain.enums.AuditAction;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class AuditActionConverter implements AttributeConverter<AuditAction, String> {

    @Override
    public String convertToDatabaseColumn(AuditAction attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public AuditAction convertToEntityAttribute(String dbData) {
        return dbData == null ? null : AuditAction.fromCode(dbData);
    }
}
