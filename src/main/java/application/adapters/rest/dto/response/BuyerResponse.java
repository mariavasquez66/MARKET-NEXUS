package application.adapters.rest.dto.response;

import java.util.List;

public record BuyerResponse(
        String id,
        String userId,
        String fullName,
        String email,
        AddressResponse mainAddress,
        List<AddressResponse> additionalAddresses,
        String commercialStatus) {
}
