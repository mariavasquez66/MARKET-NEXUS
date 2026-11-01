package application.domain.exceptions.returns;

import application.domain.exceptions.common.DomainException;

public class ReturnNotAllowedException extends DomainException {
    public ReturnNotAllowedException(String message) {
        super(message);
    }

    public ReturnNotAllowedException(String message, Throwable cause) {
        super(message, cause);
    }
}
