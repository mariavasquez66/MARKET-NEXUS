package application.adapters.rest.dto.response;

public record WarehouseResponse(
        String id,
        String name,
        AddressResponse location,
        String type,
        String ownerSellerId,
        String responsibleUserId) {
}
