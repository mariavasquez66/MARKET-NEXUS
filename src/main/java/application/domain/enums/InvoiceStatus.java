package application.domain.enums;

import application.domain.exceptions.common.InvalidDataException;
import java.util.HashMap;
import java.util.Map;

public class InvoiceStatus extends DomainCatalog {
    public static final InvoiceStatus ISSUED = new InvoiceStatus("ISSUED", "Issued");
    public static final InvoiceStatus VOIDED = new InvoiceStatus("VOIDED", "Voided");

    private static final Map<String, InvoiceStatus> VALUES = new HashMap<>();

    static {
        VALUES.put("ISSUED", ISSUED);
        VALUES.put("VOIDED", VOIDED);
    }

    private InvoiceStatus(String code, String description) {
        super(code, description);
    }

    public static InvoiceStatus fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new InvalidDataException("InvoiceStatus code cannot be null or blank");
        }
        InvoiceStatus status = VALUES.get(code.trim().toUpperCase());
        if (status == null) {
            throw new InvalidDataException("Invalid InvoiceStatus code: " + code);
        }
        return status;
    }

    public static InvoiceStatus[] values() {
        return new InvoiceStatus[]{ISSUED, VOIDED};
    }
}
