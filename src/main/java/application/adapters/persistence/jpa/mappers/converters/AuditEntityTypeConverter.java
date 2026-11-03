package application.adapters.persistence.jpa.mappers.converters;

import application.domain.enums.AuditEntityType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class AuditEntityTypeConverter implements AttributeConverter<AuditEntityType, String> {

    @Override
    public String convertToDatabaseColumn(AuditEntityType attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public AuditEntityType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : AuditEntityType.fromCode(dbData);
    }
}
