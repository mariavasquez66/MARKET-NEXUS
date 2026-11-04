package application.adapters.rest.dto.response;

public record ShipmentResponse(
        String id,
        String orderId,
        String originWarehouseId,
        AddressResponse destination,
        String trackingNumber,
        String status) {
}
