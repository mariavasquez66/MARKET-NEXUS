package application.domain.valueobjects;

import application.domain.enums.VariantType;
import application.domain.exceptions.common.InvalidDataException;
import java.util.Objects;

public final class ProductVariant {
    private static final int MAX_VALUE_LENGTH = 50;

    private final VariantType type;
    private final String value;

    private ProductVariant(VariantType type, String value) {
        this.type = type;
        this.value = value;
    }

    public static ProductVariant of(VariantType type, String value) {
        if (type == null) {
            throw new InvalidDataException("Variant type is required");
        }
        if (value == null || value.isBlank()) {
            throw new InvalidDataException("Variant value is required");
        }
        String normalized = value.trim();
        if (normalized.length() > MAX_VALUE_LENGTH) {
            throw new InvalidDataException("Variant value cannot exceed 50 characters");
        }
        return new ProductVariant(type, normalized);
    }

    public VariantType getType() {
        return type;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ProductVariant that)) {
            return false;
        }
        return type.equals(that.type) && value.equalsIgnoreCase(that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, value.toLowerCase());
    }

    @Override
    public String toString() {
        return type.getCode() + ": " + value;
    }
}
