package application.domain.enums;

import application.domain.exceptions.common.InvalidDataException;
import java.util.HashMap;
import java.util.Map;

public class PaymentStatus extends DomainCatalog {
    public static final PaymentStatus PENDING = new PaymentStatus("PENDING", "Pending");
    public static final PaymentStatus CONFIRMED = new PaymentStatus("CONFIRMED", "Confirmed");
    public static final PaymentStatus FAILED = new PaymentStatus("FAILED", "Failed");
    public static final PaymentStatus REFUNDED = new PaymentStatus("REFUNDED", "Refunded");

    private static final Map<String, PaymentStatus> VALUES = new HashMap<>();

    static {
        VALUES.put("PENDING", PENDING);
        VALUES.put("CONFIRMED", CONFIRMED);
        VALUES.put("FAILED", FAILED);
        VALUES.put("REFUNDED", REFUNDED);
    }

    private PaymentStatus(String code, String description) {
        super(code, description);
    }

    public static PaymentStatus fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new InvalidDataException("PaymentStatus code cannot be null or blank");
        }
        PaymentStatus status = VALUES.get(code.toUpperCase());
        if (status == null) {
            throw new InvalidDataException("Invalid PaymentStatus code: " + code);
        }
        return status;
    }
}
