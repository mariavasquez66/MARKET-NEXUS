package application.domain.enums;

import application.domain.exceptions.common.InvalidDataException;
import java.util.HashMap;
import java.util.Map;

public class AuditEntityType extends DomainCatalog {
    public static final AuditEntityType USER = new AuditEntityType("USER", "User");
    public static final AuditEntityType BUYER = new AuditEntityType("BUYER", "Buyer");
    public static final AuditEntityType SELLER = new AuditEntityType("SELLER", "Seller");
    public static final AuditEntityType WAREHOUSE = new AuditEntityType("WAREHOUSE", "Warehouse");
    public static final AuditEntityType PRODUCT = new AuditEntityType("PRODUCT", "Product");
    public static final AuditEntityType INVENTORY = new AuditEntityType("INVENTORY", "Inventory");
    public static final AuditEntityType INVENTORY_MOVEMENT =
            new AuditEntityType("INVENTORY_MOVEMENT", "Inventory Movement");
    public static final AuditEntityType CART = new AuditEntityType("CART", "Cart");
    public static final AuditEntityType ORDER = new AuditEntityType("ORDER", "Order");
    public static final AuditEntityType INVOICE = new AuditEntityType("INVOICE", "Invoice");
    public static final AuditEntityType SHIPMENT = new AuditEntityType("SHIPMENT", "Shipment");
    public static final AuditEntityType RETURN = new AuditEntityType("RETURN", "Return");
    public static final AuditEntityType REFUND = new AuditEntityType("REFUND", "Refund");

    private static final Map<String, AuditEntityType> VALUES = new HashMap<>();

    static {
        VALUES.put("USER", USER);
        VALUES.put("BUYER", BUYER);
        VALUES.put("SELLER", SELLER);
        VALUES.put("WAREHOUSE", WAREHOUSE);
        VALUES.put("PRODUCT", PRODUCT);
        VALUES.put("INVENTORY", INVENTORY);
        VALUES.put("INVENTORY_MOVEMENT", INVENTORY_MOVEMENT);
        VALUES.put("CART", CART);
        VALUES.put("ORDER", ORDER);
        VALUES.put("INVOICE", INVOICE);
        VALUES.put("SHIPMENT", SHIPMENT);
        VALUES.put("RETURN", RETURN);
        VALUES.put("REFUND", REFUND);
    }

    private AuditEntityType(String code, String description) {
        super(code, description);
    }

    public static AuditEntityType fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new InvalidDataException("AuditEntityType code cannot be null or blank");
        }
        AuditEntityType type = VALUES.get(code.trim().toUpperCase());
        if (type == null) {
            throw new InvalidDataException("Invalid AuditEntityType code: " + code);
        }
        return type;
    }

    public static AuditEntityType[] values() {
        return new AuditEntityType[]{USER, BUYER, SELLER, WAREHOUSE, PRODUCT, INVENTORY,
                INVENTORY_MOVEMENT, CART, ORDER, INVOICE, SHIPMENT, RETURN, REFUND};
    }
}
