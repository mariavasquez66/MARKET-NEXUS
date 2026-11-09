package application.adapters.rest.mappers;

import application.adapters.rest.dto.response.AddressResponse;
import application.adapters.rest.dto.response.BuyerResponse;
import application.domain.models.Buyer;
import application.domain.models.User;
import application.domain.valueobjects.Address;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class BuyerRestMapper extends RestMapperSupport {

    public BuyerResponse toResponse(Buyer buyer) {
        User user = buyer.getUser();
        List<AddressResponse> additionalAddresses = buyer.getAdditionalAddresses().stream()
                .map(this::toAddressResponse)
                .toList();
        return new BuyerResponse(
                buyer.getId(),
                user.getId(),
                user.getFullName().getValue(),
                user.getEmail().getValue(),
                toAddressResponse(buyer.getMainAddress()),
                List.copyOf(additionalAddresses),
                buyer.getCommercialStatus().getCode());
    }

    public List<AddressResponse> toAddressResponses(List<Address> addresses) {
        return addresses.stream()
                .map(this::toAddressResponse)
                .toList();
    }
}
