package application.adapters.persistence.jpa.mappers.converters;

import application.domain.enums.CommercialStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class CommercialStatusConverter implements AttributeConverter<CommercialStatus, String> {

    @Override
    public String convertToDatabaseColumn(CommercialStatus attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public CommercialStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : CommercialStatus.fromCode(dbData);
    }
}
