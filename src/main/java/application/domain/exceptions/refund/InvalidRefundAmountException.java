package application.domain.exceptions.refund;

import application.domain.exceptions.common.DomainException;

public class InvalidRefundAmountException extends DomainException {
    public InvalidRefundAmountException(String message) {
        super(message);
    }

    public InvalidRefundAmountException(String message, Throwable cause) {
        super(message, cause);
    }
}
