package application.domain.enums;

import application.domain.exceptions.common.InvalidDataException;
import java.util.HashMap;
import java.util.Map;

public class ProductType extends DomainCatalog {
    public static final ProductType PHYSICAL = new ProductType("PHYSICAL", "Physical");
    public static final ProductType DIGITAL = new ProductType("DIGITAL", "Digital");

    private static final Map<String, ProductType> VALUES = new HashMap<>();

    static {
        VALUES.put("PHYSICAL", PHYSICAL);
        VALUES.put("DIGITAL", DIGITAL);
    }

    private ProductType(String code, String description) {
        super(code, description);
    }

    public static ProductType fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new InvalidDataException("ProductType code cannot be null or blank");
        }
        ProductType type = VALUES.get(code.trim().toUpperCase());
        if (type == null) {
            throw new InvalidDataException("Invalid ProductType code: " + code);
        }
        return type;
    }

    public static ProductType[] values() {
        return new ProductType[]{PHYSICAL, DIGITAL};
    }
}
