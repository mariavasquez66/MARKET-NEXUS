package application.domain.exceptions.inventory;

import application.domain.exceptions.common.DomainException;

public class DamagedInventoryException extends DomainException {
    public DamagedInventoryException(String message) {
        super(message);
    }

    public DamagedInventoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
