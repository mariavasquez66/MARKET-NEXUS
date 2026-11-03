package application.domain.exceptions.returns;

import application.domain.exceptions.common.DomainException;

public class ReturnAlreadyProcessedException extends DomainException {
    public ReturnAlreadyProcessedException(String message) {
        super(message);
    }

    public ReturnAlreadyProcessedException(String message, Throwable cause) {
        super(message, cause);
    }
}
