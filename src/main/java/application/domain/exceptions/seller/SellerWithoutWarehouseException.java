package application.domain.exceptions.seller;

import application.domain.exceptions.common.DomainException;

public class SellerWithoutWarehouseException extends DomainException {
    public SellerWithoutWarehouseException(String message) {
        super(message);
    }

    public SellerWithoutWarehouseException(String message, Throwable cause) {
        super(message, cause);
    }
}
