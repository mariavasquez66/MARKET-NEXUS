package application.adapters.persistence.jpa.mappers.converters;

import application.domain.enums.MovementType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class MovementTypeConverter implements AttributeConverter<MovementType, String> {

    @Override
    public String convertToDatabaseColumn(MovementType attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public MovementType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : MovementType.fromCode(dbData);
    }
}
