package application.domain.exceptions.refund;

import application.domain.exceptions.common.DomainException;

public class RefundNotFoundException extends DomainException {
    public RefundNotFoundException(String message) {
        super(message);
    }

    public RefundNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
