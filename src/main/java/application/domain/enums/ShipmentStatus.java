package application.domain.enums;

import application.domain.exceptions.common.InvalidDataException;
import java.util.HashMap;
import java.util.Map;

public class ShipmentStatus extends DomainCatalog {
    public static final ShipmentStatus PENDING = new ShipmentStatus("PENDING", "Pending");
    public static final ShipmentStatus IN_TRANSIT = new ShipmentStatus("IN_TRANSIT", "In Transit");
    public static final ShipmentStatus DELIVERED = new ShipmentStatus("DELIVERED", "Delivered");

    private static final Map<String, ShipmentStatus> VALUES = new HashMap<>();

    static {
        VALUES.put("PENDING", PENDING);
        VALUES.put("IN_TRANSIT", IN_TRANSIT);
        VALUES.put("DELIVERED", DELIVERED);
    }

    private ShipmentStatus(String code, String description) {
        super(code, description);
    }

    public static ShipmentStatus fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new InvalidDataException("ShipmentStatus code cannot be null or blank");
        }
        ShipmentStatus status = VALUES.get(code.toUpperCase());
        if (status == null) {
            throw new InvalidDataException("Invalid ShipmentStatus code: " + code);
        }
        return status;
    }
}
