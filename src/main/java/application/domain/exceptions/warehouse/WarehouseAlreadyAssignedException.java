package application.domain.exceptions.warehouse;

import application.domain.exceptions.common.DomainException;

public class WarehouseAlreadyAssignedException extends DomainException {
    public WarehouseAlreadyAssignedException(String message) {
        super(message);
    }

    public WarehouseAlreadyAssignedException(String message, Throwable cause) {
        super(message, cause);
    }
}
