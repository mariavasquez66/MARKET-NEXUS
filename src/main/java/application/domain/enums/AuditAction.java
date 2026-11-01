package application.domain.enums;

import application.domain.exceptions.common.InvalidDataException;
import java.util.HashMap;
import java.util.Map;

public class AuditAction extends DomainCatalog {
    public static final AuditAction USER_CREATED = new AuditAction("USER_CREATED", "User Created");
    public static final AuditAction USER_BLOCKED = new AuditAction("USER_BLOCKED", "User Blocked");
    public static final AuditAction USER_ACTIVATED = new AuditAction("USER_ACTIVATED", "User Activated");
    public static final AuditAction USER_DEACTIVATED = new AuditAction("USER_DEACTIVATED", "User Deactivated");
    public static final AuditAction SELLER_REGISTERED = new AuditAction("SELLER_REGISTERED", "Seller Registered");
    public static final AuditAction SELLER_TAX_ID_UPDATED =
            new AuditAction("SELLER_TAX_ID_UPDATED", "Seller Tax Id Updated");
    public static final AuditAction WAREHOUSE_CREATED =
            new AuditAction("WAREHOUSE_CREATED", "Warehouse Created");
    public static final AuditAction WAREHOUSE_UPDATED =
            new AuditAction("WAREHOUSE_UPDATED", "Warehouse Updated");
    public static final AuditAction WAREHOUSE_ASSIGNED =
            new AuditAction("WAREHOUSE_ASSIGNED", "Warehouse Assigned");
    public static final AuditAction PRODUCT_REGISTERED =
            new AuditAction("PRODUCT_REGISTERED", "Product Registered");
    public static final AuditAction PRODUCT_UPDATED = new AuditAction("PRODUCT_UPDATED", "Product Updated");
    public static final AuditAction PRODUCT_PUBLISHED =
            new AuditAction("PRODUCT_PUBLISHED", "Product Published");
    public static final AuditAction PRODUCT_SUSPENDED =
            new AuditAction("PRODUCT_SUSPENDED", "Product Suspended");
    public static final AuditAction PRODUCT_DISCONTINUED =
            new AuditAction("PRODUCT_DISCONTINUED", "Product Discontinued");
    public static final AuditAction INVENTORY_MOVEMENT =
            new AuditAction("INVENTORY_MOVEMENT", "Inventory Movement");
    public static final AuditAction ORDER_CREATED = new AuditAction("ORDER_CREATED", "Order Created");
    public static final AuditAction ORDER_STATUS_CHANGED =
            new AuditAction("ORDER_STATUS_CHANGED", "Order Status Changed");
    public static final AuditAction INVOICE_ISSUED = new AuditAction("INVOICE_ISSUED", "Invoice Issued");
    public static final AuditAction INVOICE_VOIDED = new AuditAction("INVOICE_VOIDED", "Invoice Voided");
    public static final AuditAction SHIPMENT_CREATED =
            new AuditAction("SHIPMENT_CREATED", "Shipment Created");
    public static final AuditAction SHIPMENT_STARTED =
            new AuditAction("SHIPMENT_STARTED", "Shipment Started");
    public static final AuditAction SHIPMENT_DELIVERED =
            new AuditAction("SHIPMENT_DELIVERED", "Shipment Delivered");
    public static final AuditAction RETURN_REQUESTED = new AuditAction("RETURN_REQUESTED", "Return Requested");
    public static final AuditAction RETURN_APPROVED = new AuditAction("RETURN_APPROVED", "Return Approved");
    public static final AuditAction RETURN_REJECTED = new AuditAction("RETURN_REJECTED", "Return Rejected");
    public static final AuditAction RETURN_COMPLETED = new AuditAction("RETURN_COMPLETED", "Return Completed");
    public static final AuditAction REFUND_CREATED = new AuditAction("REFUND_CREATED", "Refund Created");
    public static final AuditAction REFUND_PROCESSED = new AuditAction("REFUND_PROCESSED", "Refund Processed");

    private static final Map<String, AuditAction> VALUES = new HashMap<>();

    static {
        VALUES.put("USER_CREATED", USER_CREATED);
        VALUES.put("USER_BLOCKED", USER_BLOCKED);
        VALUES.put("USER_ACTIVATED", USER_ACTIVATED);
        VALUES.put("USER_DEACTIVATED", USER_DEACTIVATED);
        VALUES.put("SELLER_REGISTERED", SELLER_REGISTERED);
        VALUES.put("SELLER_TAX_ID_UPDATED", SELLER_TAX_ID_UPDATED);
        VALUES.put("WAREHOUSE_CREATED", WAREHOUSE_CREATED);
        VALUES.put("WAREHOUSE_UPDATED", WAREHOUSE_UPDATED);
        VALUES.put("WAREHOUSE_ASSIGNED", WAREHOUSE_ASSIGNED);
        VALUES.put("PRODUCT_REGISTERED", PRODUCT_REGISTERED);
        VALUES.put("PRODUCT_UPDATED", PRODUCT_UPDATED);
        VALUES.put("PRODUCT_PUBLISHED", PRODUCT_PUBLISHED);
        VALUES.put("PRODUCT_SUSPENDED", PRODUCT_SUSPENDED);
        VALUES.put("PRODUCT_DISCONTINUED", PRODUCT_DISCONTINUED);
        VALUES.put("INVENTORY_MOVEMENT", INVENTORY_MOVEMENT);
        VALUES.put("ORDER_CREATED", ORDER_CREATED);
        VALUES.put("ORDER_STATUS_CHANGED", ORDER_STATUS_CHANGED);
        VALUES.put("INVOICE_ISSUED", INVOICE_ISSUED);
        VALUES.put("INVOICE_VOIDED", INVOICE_VOIDED);
        VALUES.put("SHIPMENT_CREATED", SHIPMENT_CREATED);
        VALUES.put("SHIPMENT_STARTED", SHIPMENT_STARTED);
        VALUES.put("SHIPMENT_DELIVERED", SHIPMENT_DELIVERED);
        VALUES.put("RETURN_REQUESTED", RETURN_REQUESTED);
        VALUES.put("RETURN_APPROVED", RETURN_APPROVED);
        VALUES.put("RETURN_REJECTED", RETURN_REJECTED);
        VALUES.put("RETURN_COMPLETED", RETURN_COMPLETED);
        VALUES.put("REFUND_CREATED", REFUND_CREATED);
        VALUES.put("REFUND_PROCESSED", REFUND_PROCESSED);
    }

    private AuditAction(String code, String description) {
        super(code, description);
    }

    public static AuditAction fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new InvalidDataException("AuditAction code cannot be null or blank");
        }
        AuditAction action = VALUES.get(code.trim().toUpperCase());
        if (action == null) {
            throw new InvalidDataException("Invalid AuditAction code: " + code);
        }
        return action;
    }

    public static AuditAction[] values() {
        return new AuditAction[]{USER_CREATED, USER_BLOCKED, USER_ACTIVATED, USER_DEACTIVATED,
                SELLER_REGISTERED, SELLER_TAX_ID_UPDATED, WAREHOUSE_CREATED, WAREHOUSE_UPDATED,
                WAREHOUSE_ASSIGNED, PRODUCT_REGISTERED, PRODUCT_UPDATED, PRODUCT_PUBLISHED,
                PRODUCT_SUSPENDED, PRODUCT_DISCONTINUED, INVENTORY_MOVEMENT, ORDER_CREATED,
                ORDER_STATUS_CHANGED, INVOICE_ISSUED, INVOICE_VOIDED, SHIPMENT_CREATED,
                SHIPMENT_STARTED, SHIPMENT_DELIVERED, RETURN_REQUESTED, RETURN_APPROVED,
                RETURN_REJECTED, RETURN_COMPLETED, REFUND_CREATED, REFUND_PROCESSED};
    }
}
