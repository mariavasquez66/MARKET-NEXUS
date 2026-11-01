package application.domain.enums;

import application.domain.exceptions.common.InvalidDataException;
import java.util.HashMap;
import java.util.Map;

public class MovementType extends DomainCatalog {
    public static final MovementType INFLOW = new MovementType("INFLOW", "Inflow");
    public static final MovementType RESERVATION = new MovementType("RESERVATION", "Reservation");
    public static final MovementType SALE = new MovementType("SALE", "Sale");
    public static final MovementType ADJUSTMENT = new MovementType("ADJUSTMENT", "Adjustment");
    public static final MovementType RETURN = new MovementType("RETURN", "Return");

    private static final Map<String, MovementType> VALUES = new HashMap<>();

    static {
        VALUES.put("INFLOW", INFLOW);
        VALUES.put("RESERVATION", RESERVATION);
        VALUES.put("SALE", SALE);
        VALUES.put("ADJUSTMENT", ADJUSTMENT);
        VALUES.put("RETURN", RETURN);
    }

    private MovementType(String code, String description) {
        super(code, description);
    }

    public static MovementType fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new InvalidDataException("MovementType code cannot be null or blank");
        }
        MovementType type = VALUES.get(code.trim().toUpperCase());
        if (type == null) {
            throw new InvalidDataException("Invalid MovementType code: " + code);
        }
        return type;
    }

    public static MovementType[] values() {
        return new MovementType[]{INFLOW, RESERVATION, SALE, ADJUSTMENT, RETURN};
    }
}
