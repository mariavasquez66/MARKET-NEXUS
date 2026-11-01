package application.domain.enums;

import application.domain.exceptions.common.InvalidDataException;
import java.util.HashMap;
import java.util.Map;

public class RefundStatus extends DomainCatalog {
    public static final RefundStatus PENDING = new RefundStatus("PENDING", "Pending");
    public static final RefundStatus PROCESSED = new RefundStatus("PROCESSED", "Processed");

    private static final Map<String, RefundStatus> VALUES = new HashMap<>();

    static {
        VALUES.put("PENDING", PENDING);
        VALUES.put("PROCESSED", PROCESSED);
    }

    private RefundStatus(String code, String description) {
        super(code, description);
    }

    public static RefundStatus fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new InvalidDataException("RefundStatus code cannot be null or blank");
        }
        RefundStatus status = VALUES.get(code.trim().toUpperCase());
        if (status == null) {
            throw new InvalidDataException("Invalid RefundStatus code: " + code);
        }
        return status;
    }

    public static RefundStatus[] values() {
        return new RefundStatus[]{PENDING, PROCESSED};
    }
}
