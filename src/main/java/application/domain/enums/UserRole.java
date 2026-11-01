package application.domain.enums;

import application.domain.exceptions.common.InvalidDataException;
import java.util.HashMap;
import java.util.Map;

public class UserRole extends DomainCatalog {
    public static final UserRole BUYER = new UserRole("BUYER", "Buyer");
    public static final UserRole SELLER = new UserRole("SELLER", "Seller");
    public static final UserRole LOGISTIC_OPERATOR = new UserRole("LOGISTIC_OPERATOR", "Logistic Operator");
    public static final UserRole ADMIN = new UserRole("ADMIN", "Administrator");
    public static final UserRole SUPERVISOR = new UserRole("SUPERVISOR", "Supervisor");

    private static final Map<String, UserRole> VALUES = new HashMap<>();

    static {
        VALUES.put("BUYER", BUYER);
        VALUES.put("SELLER", SELLER);
        VALUES.put("LOGISTIC_OPERATOR", LOGISTIC_OPERATOR);
        VALUES.put("ADMIN", ADMIN);
        VALUES.put("SUPERVISOR", SUPERVISOR);
    }

    private UserRole(String code, String description) {
        super(code, description);
    }

    public static UserRole fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new InvalidDataException("UserRole code cannot be null or blank");
        }
        UserRole role = VALUES.get(code.toUpperCase());
        if (role == null) {
            throw new InvalidDataException("Invalid UserRole code: " + code);
        }
        return role;
    }

    public static UserRole[] values() {
        return new UserRole[]{BUYER, SELLER, LOGISTIC_OPERATOR, ADMIN, SUPERVISOR};
    }
}
