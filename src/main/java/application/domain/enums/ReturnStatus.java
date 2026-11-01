package application.domain.enums;

import application.domain.exceptions.common.InvalidDataException;
import java.util.HashMap;
import java.util.Map;

public class ReturnStatus extends DomainCatalog {
    public static final ReturnStatus REQUESTED = new ReturnStatus("REQUESTED", "Requested");
    public static final ReturnStatus APPROVED = new ReturnStatus("APPROVED", "Approved");
    public static final ReturnStatus REJECTED = new ReturnStatus("REJECTED", "Rejected");
    public static final ReturnStatus COMPLETED = new ReturnStatus("COMPLETED", "Completed");

    private static final Map<String, ReturnStatus> VALUES = new HashMap<>();

    static {
        VALUES.put("REQUESTED", REQUESTED);
        VALUES.put("APPROVED", APPROVED);
        VALUES.put("REJECTED", REJECTED);
        VALUES.put("COMPLETED", COMPLETED);
    }

    private ReturnStatus(String code, String description) {
        super(code, description);
    }

    public static ReturnStatus fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new InvalidDataException("ReturnStatus code cannot be null or blank");
        }
        ReturnStatus status = VALUES.get(code.trim().toUpperCase());
        if (status == null) {
            throw new InvalidDataException("Invalid ReturnStatus code: " + code);
        }
        return status;
    }

    public static ReturnStatus[] values() {
        return new ReturnStatus[]{REQUESTED, APPROVED, REJECTED, COMPLETED};
    }
}
