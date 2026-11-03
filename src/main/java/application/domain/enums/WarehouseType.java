package application.domain.enums;

import application.domain.exceptions.common.InvalidDataException;
import java.util.HashMap;
import java.util.Map;

public class WarehouseType extends DomainCatalog {
    public static final WarehouseType MARKETPLACE = new WarehouseType("MARKETPLACE", "Marketplace");
    public static final WarehouseType SELLER = new WarehouseType("SELLER", "Seller");

    private static final Map<String, WarehouseType> VALUES = new HashMap<>();

    static {
        VALUES.put("MARKETPLACE", MARKETPLACE);
        VALUES.put("SELLER", SELLER);
    }

    private WarehouseType(String code, String description) {
        super(code, description);
    }

    public static WarehouseType fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new InvalidDataException("WarehouseType code cannot be null or blank");
        }
        WarehouseType type = VALUES.get(code.trim().toUpperCase());
        if (type == null) {
            throw new InvalidDataException("Invalid WarehouseType code: " + code);
        }
        return type;
    }

    public static WarehouseType[] values() {
        return new WarehouseType[]{MARKETPLACE, SELLER};
    }
}
