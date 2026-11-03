package application.domain.exceptions.inventory;

import application.domain.exceptions.common.DomainException;

public class InventoryNotFoundException extends DomainException {
    public InventoryNotFoundException(String message) {
        super(message);
    }

    public InventoryNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
