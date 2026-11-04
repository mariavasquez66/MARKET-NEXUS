package application.adapters.rest.dto.response;

public record AddressResponse(
        String street,
        String city,
        String region,
        String postalCode,
        String country) {
}
