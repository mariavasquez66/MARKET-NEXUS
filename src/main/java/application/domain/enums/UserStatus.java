package application.domain.enums;

import application.domain.exceptions.common.InvalidDataException;
import java.util.HashMap;
import java.util.Map;

public class UserStatus extends DomainCatalog {
    public static final UserStatus ACTIVE = new UserStatus("ACTIVE", "Active");
    public static final UserStatus BLOCKED = new UserStatus("BLOCKED", "Blocked");
    public static final UserStatus INACTIVE = new UserStatus("INACTIVE", "Inactive");

    private static final Map<String, UserStatus> VALUES = new HashMap<>();

    static {
        VALUES.put("ACTIVE", ACTIVE);
        VALUES.put("BLOCKED", BLOCKED);
        VALUES.put("INACTIVE", INACTIVE);
    }

    private UserStatus(String code, String description) {
        super(code, description);
    }

    public static UserStatus fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new InvalidDataException("UserStatus code cannot be null or blank");
        }
        UserStatus status = VALUES.get(code.toUpperCase());
        if (status == null) {
            throw new InvalidDataException("Invalid UserStatus code: " + code);
        }
        return status;
    }
}
