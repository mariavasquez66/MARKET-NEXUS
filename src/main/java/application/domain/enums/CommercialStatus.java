package application.domain.enums;

import application.domain.exceptions.common.InvalidDataException;
import java.util.HashMap;
import java.util.Map;

public class CommercialStatus extends DomainCatalog {
    public static final CommercialStatus ACTIVE = new CommercialStatus("ACTIVE", "Active");
    public static final CommercialStatus RESTRICTED = new CommercialStatus("RESTRICTED", "Restricted");

    private static final Map<String, CommercialStatus> VALUES = new HashMap<>();

    static {
        VALUES.put("ACTIVE", ACTIVE);
        VALUES.put("RESTRICTED", RESTRICTED);
    }

    private CommercialStatus(String code, String description) {
        super(code, description);
    }

    public static CommercialStatus fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new InvalidDataException("CommercialStatus code cannot be null or blank");
        }
        CommercialStatus status = VALUES.get(code.toUpperCase());
        if (status == null) {
            throw new InvalidDataException("Invalid CommercialStatus code: " + code);
        }
        return status;
    }
}
