package application.adapters.persistence.jpa.mappers.converters;

import application.domain.enums.ShipmentStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class ShipmentStatusConverter implements AttributeConverter<ShipmentStatus, String> {

    @Override
    public String convertToDatabaseColumn(ShipmentStatus attribute) {
        return attribute == null ? null : attribute.getCode();
    }

    @Override
    public ShipmentStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : ShipmentStatus.fromCode(dbData);
    }
}
