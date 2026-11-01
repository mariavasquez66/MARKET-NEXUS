package application.domain.exceptions.warehouse;

import application.domain.exceptions.common.DomainException;

public class InvalidWarehouseTypeException extends DomainException {
    public InvalidWarehouseTypeException(String message) {
        super(message);
    }

    public InvalidWarehouseTypeException(String message, Throwable cause) {
        super(message, cause);
    }
}
