package application.adapters.rest.mappers;

import application.adapters.rest.dto.request.AddressRequest;
import application.adapters.rest.dto.response.AddressResponse;
import application.adapters.rest.dto.response.MoneyResponse;
import application.domain.valueobjects.Address;
import application.domain.valueobjects.Money;
import java.math.BigDecimal;

abstract class RestMapperSupport {

    public Address toAddress(AddressRequest request) {
        if (request == null) {
            return null;
        }
        return Address.of(
                request.street(),
                request.city(),
                request.region(),
                request.postalCode(),
                request.country());
    }

    public Money toMoney(BigDecimal amount) {
        return amount == null ? null : Money.of(amount);
    }

    protected MoneyResponse toMoneyResponse(Money money) {
        if (money == null) {
            return null;
        }
        return new MoneyResponse(money.getAmount(), money.getCurrency().getCurrencyCode());
    }

    protected AddressResponse toAddressResponse(Address address) {
        if (address == null) {
            return null;
        }
        return new AddressResponse(
                address.getStreet(),
                address.getCity(),
                address.getRegion(),
                address.getPostalCode(),
                address.getCountry());
    }
}
