package application.domain.exceptions.order;

import application.domain.exceptions.common.DomainException;

public class FinalizedOrderException extends DomainException {
    public FinalizedOrderException(String message) {
        super(message);
    }

    public FinalizedOrderException(String message, Throwable cause) {
        super(message, cause);
    }
}
