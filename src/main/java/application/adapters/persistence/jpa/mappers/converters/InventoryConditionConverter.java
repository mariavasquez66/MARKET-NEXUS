package application.adapters.persistence.jpa.mappers.converters;

import application.domain.enums.InventoryCondition;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class InventoryConditionConverter implements AttributeConverter<InventoryCondition, String> {

    @Override
    public String convertToDatabaseColumn(InventoryCondition attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public InventoryCondition convertToEntityAttribute(String dbData) {
        return dbData == null ? null : InventoryCondition.fromCode(dbData);
    }
}
