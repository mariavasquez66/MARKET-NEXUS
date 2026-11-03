package application.domain.exceptions.returns;

import application.domain.exceptions.common.DomainException;

public class ReturnNotFoundException extends DomainException {
    public ReturnNotFoundException(String message) {
        super(message);
    }

    public ReturnNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
