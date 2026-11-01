package application.domain.exceptions.refund;

import application.domain.exceptions.common.DomainException;

public class RefundAlreadyProcessedException extends DomainException {
    public RefundAlreadyProcessedException(String message) {
        super(message);
    }

    public RefundAlreadyProcessedException(String message, Throwable cause) {
        super(message, cause);
    }
}
