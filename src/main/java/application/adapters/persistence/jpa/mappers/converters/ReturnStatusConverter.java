package application.adapters.persistence.jpa.mappers.converters;

import application.domain.enums.ReturnStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ReturnStatusConverter implements AttributeConverter<ReturnStatus, String> {

    @Override
    public String convertToDatabaseColumn(ReturnStatus attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public ReturnStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : ReturnStatus.fromCode(dbData);
    }
}
