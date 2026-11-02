package application.adapters.persistence.jpa.mappers.converters;

import application.domain.enums.WarehouseType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class WarehouseTypeConverter implements AttributeConverter<WarehouseType, String> {

    @Override
    public String convertToDatabaseColumn(WarehouseType attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public WarehouseType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : WarehouseType.fromCode(dbData);
    }
}
