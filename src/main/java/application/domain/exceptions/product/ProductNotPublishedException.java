package application.domain.exceptions.product;

import application.domain.exceptions.common.DomainException;

public class ProductNotPublishedException extends DomainException {
    public ProductNotPublishedException(String message) {
        super(message);
    }

    public ProductNotPublishedException(String message, Throwable cause) {
        super(message, cause);
    }
}
