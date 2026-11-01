package application.domain.enums;

import application.domain.exceptions.common.InvalidDataException;
import java.util.HashMap;
import java.util.Map;

public class InventoryCondition extends DomainCatalog {
    public static final InventoryCondition GOOD = new InventoryCondition("GOOD", "Good");
    public static final InventoryCondition DAMAGED = new InventoryCondition("DAMAGED", "Damaged");

    private static final Map<String, InventoryCondition> VALUES = new HashMap<>();

    static {
        VALUES.put("GOOD", GOOD);
        VALUES.put("DAMAGED", DAMAGED);
    }

    private InventoryCondition(String code, String description) {
        super(code, description);
    }

    public static InventoryCondition fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new InvalidDataException("InventoryCondition code cannot be null or blank");
        }
        InventoryCondition condition = VALUES.get(code.trim().toUpperCase());
        if (condition == null) {
            throw new InvalidDataException("Invalid InventoryCondition code: " + code);
        }
        return condition;
    }

    public static InventoryCondition[] values() {
        return new InventoryCondition[]{GOOD, DAMAGED};
    }
}
