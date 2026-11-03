package application.adapters.persistence.jpa.mappers;

import application.adapters.persistence.jpa.entities.AddressEmbeddable;
import application.adapters.persistence.jpa.entities.WarehouseJpaEntity;
import application.domain.models.Seller;
import application.domain.models.User;
import application.domain.models.Warehouse;
import application.domain.valueobjects.Address;
import org.springframework.stereotype.Component;

@Component
public class WarehouseJpaMapper {

    public WarehouseJpaEntity toEntity(Warehouse warehouse) {
        WarehouseJpaEntity entity = new WarehouseJpaEntity();
        entity.setId(warehouse.getId());
        entity.setName(warehouse.getName());
        entity.setLocation(toEmbeddable(warehouse.getLocation()));
        entity.setTypeCode(warehouse.getType());
        entity.setOwnerSellerId(warehouse.getOwner() == null ? null : warehouse.getOwner().getId());
        entity.setResponsibleUserId(warehouse.getResponsible().getId());
        return entity;
    }

    public Warehouse toDomain(WarehouseJpaEntity entity, Seller owner, User responsible) {
        return Warehouse.reconstitute(
                entity.getId(),
                entity.getName(),
                toAddress(entity.getLocation()),
                entity.getTypeCode(),
                owner,
                responsible);
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
