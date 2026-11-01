package application.domain.exceptions.warehouse;

import application.domain.exceptions.common.DomainException;

public class WarehouseNotFoundException extends DomainException {
    public WarehouseNotFoundException(String message) {
        super(message);
    }

    public WarehouseNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
