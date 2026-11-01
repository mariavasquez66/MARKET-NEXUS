package application.domain.exceptions.seller;

import application.domain.exceptions.common.DomainException;

public class SellerNotFoundException extends DomainException {
    public SellerNotFoundException(String message) {
        super(message);
    }

    public SellerNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
