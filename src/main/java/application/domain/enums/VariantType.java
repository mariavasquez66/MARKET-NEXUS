package application.domain.enums;

import application.domain.exceptions.common.InvalidDataException;
import java.util.HashMap;
import java.util.Map;

public class VariantType extends DomainCatalog {
    public static final VariantType COLOR = new VariantType("COLOR", "Color");
    public static final VariantType SIZE = new VariantType("SIZE", "Size");
    public static final VariantType MODEL = new VariantType("MODEL", "Model");
    public static final VariantType OTHER = new VariantType("OTHER", "Other");

    private static final Map<String, VariantType> VALUES = new HashMap<>();

    static {
        VALUES.put("COLOR", COLOR);
        VALUES.put("SIZE", SIZE);
        VALUES.put("MODEL", MODEL);
        VALUES.put("OTHER", OTHER);
    }

    private VariantType(String code, String description) {
        super(code, description);
    }

    public static VariantType fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new InvalidDataException("VariantType code cannot be null or blank");
        }
        VariantType type = VALUES.get(code.trim().toUpperCase());
        if (type == null) {
            throw new InvalidDataException("Invalid VariantType code: " + code);
        }
        return type;
    }

    public static VariantType[] values() {
        return new VariantType[]{COLOR, SIZE, MODEL, OTHER};
    }
}
