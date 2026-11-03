package application.adapters.persistence.jpa.mappers.converters;

import application.domain.enums.RefundStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class RefundStatusConverter implements AttributeConverter<RefundStatus, String> {

    @Override
    public String convertToDatabaseColumn(RefundStatus attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public RefundStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : RefundStatus.fromCode(dbData);
    }
}
