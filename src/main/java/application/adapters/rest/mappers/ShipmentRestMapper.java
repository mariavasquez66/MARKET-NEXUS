package application.adapters.rest.mappers;

import application.adapters.rest.dto.response.ShipmentResponse;
import application.domain.models.Shipment;
import org.springframework.stereotype.Component;

@Component
public class ShipmentRestMapper extends RestMapperSupport {

    public ShipmentResponse toResponse(Shipment shipment) {
        return new ShipmentResponse(
                shipment.getId(),
                shipment.getOrder().getId(),
                shipment.getOriginWarehouse().getId(),
                toAddressResponse(shipment.getDestination()),
                shipment.getTrackingNumber(),
                shipment.getStatus().getCode());
    }
}
