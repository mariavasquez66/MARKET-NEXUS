package application.adapters.rest.mappers;

import application.adapters.rest.dto.response.WarehouseResponse;
import application.domain.models.Warehouse;
import org.springframework.stereotype.Component;

@Component
public class WarehouseRestMapper extends RestMapperSupport {

    public WarehouseResponse toResponse(Warehouse warehouse) {
        return new WarehouseResponse(
                warehouse.getId(),
                warehouse.getName(),
                toAddressResponse(warehouse.getLocation()),
                warehouse.getType().getCode(),
                warehouse.getOwner() == null ? null : warehouse.getOwner().getId(),
                warehouse.getResponsible().getId());
    }
}
