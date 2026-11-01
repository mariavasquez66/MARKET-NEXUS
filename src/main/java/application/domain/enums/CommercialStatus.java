package application.domain.enums;

import application.domain.exceptions.common.InvalidDataException;
import java.util.HashMap;
import java.util.Map;

public class CommercialStatus extends DomainCatalog {
    public static final CommercialStatus ELIGIBLE = new CommercialStatus("ELIGIBLE", "Eligible");
    public static final CommercialStatus RESTRICTED = new CommercialStatus("RESTRICTED", "Restricted");
    public static final CommercialStatus SUSPENDED = new CommercialStatus("SUSPENDED", "Suspended");

    private static final Map<String, CommercialStatus> VALUES = new HashMap<>();

    static {
        VALUES.put("ELIGIBLE", ELIGIBLE);
        VALUES.put("RESTRICTED", RESTRICTED);
        VALUES.put("SUSPENDED", SUSPENDED);
    }

    private CommercialStatus(String code, String description) {
        super(code, description);
    }

    public static CommercialStatus fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new InvalidDataException("CommercialStatus code cannot be null or blank");
        }
        CommercialStatus status = VALUES.get(code.trim().toUpperCase());
        if (status == null) {
            throw new InvalidDataException("Invalid CommercialStatus code: " + code);
        }
        return status;
    }

    public static CommercialStatus[] values() {
        return new CommercialStatus[]{ELIGIBLE, RESTRICTED, SUSPENDED};
    }
}
