package application.adapters.persistence.jpa.mappers;

import application.adapters.persistence.jpa.entities.AddressEmbeddable;
import application.adapters.persistence.jpa.entities.ShipmentJpaEntity;
import application.domain.models.Order;
import application.domain.models.Shipment;
import application.domain.models.Warehouse;
import application.domain.valueobjects.Address;
import org.springframework.stereotype.Component;

@Component
public class ShipmentJpaMapper {

    public ShipmentJpaEntity toEntity(Shipment shipment) {
        ShipmentJpaEntity entity = new ShipmentJpaEntity();
        entity.setId(shipment.getId());
        entity.setOrderId(shipment.getOrder().getId());
        entity.setOriginWarehouseId(shipment.getOriginWarehouse().getId());
        entity.setDestination(toEmbeddable(shipment.getDestination()));
        entity.setTrackingNumber(shipment.getTrackingNumber());
        entity.setStatusCode(shipment.getStatus());
        return entity;
    }

    public Shipment toDomain(ShipmentJpaEntity entity, Order order, Warehouse origin) {
        return Shipment.reconstitute(
                entity.getId(),
                order,
                origin,
                toAddress(entity.getDestination()),
                entity.getTrackingNumber(),
                entity.getStatusCode());
    }

    private AddressEmbeddable toEmbeddable(Address address) {
        AddressEmbeddable embeddable = new AddressEmbeddable();
        embeddable.setStreet(address.getStreet());
        embeddable.setCity(address.getCity());
        embeddable.setRegion(address.getRegion());
        embeddable.setPostalCode(address.getPostalCode());
        embeddable.setCountry(address.getCountry());
        return embeddable;
    }

    private Address toAddress(AddressEmbeddable embeddable) {
        return Address.of(
                embeddable.getStreet(),
                embeddable.getCity(),
                embeddable.getRegion(),
                embeddable.getPostalCode(),
                embeddable.getCountry());
    }
}
