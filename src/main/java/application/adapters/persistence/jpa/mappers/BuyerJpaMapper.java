package application.adapters.persistence.jpa.mappers;

import application.adapters.persistence.jpa.entities.AddressEmbeddable;
import application.adapters.persistence.jpa.entities.BuyerJpaEntity;
import application.domain.models.Buyer;
import application.domain.models.User;
import application.domain.valueobjects.Address;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class BuyerJpaMapper {

    public BuyerJpaEntity toEntity(Buyer buyer) {
        BuyerJpaEntity entity = new BuyerJpaEntity();
        entity.setId(buyer.getId());
        entity.setUserId(buyer.getUser().getId());
        entity.setMainAddress(toEmbeddable(buyer.getMainAddress()));
        entity.setAdditionalAddresses(buyer.getAdditionalAddresses().stream()
                .map(this::toEmbeddable)
                .toList());
        entity.setCommercialStatusCode(buyer.getCommercialStatus());
        return entity;
    }

    public Buyer toDomain(BuyerJpaEntity entity, User user) {
        List<Address> additionalAddresses = new ArrayList<>();
        if (entity.getAdditionalAddresses() != null) {
            for (AddressEmbeddable embeddable : entity.getAdditionalAddresses()) {
                additionalAddresses.add(toAddress(embeddable));
            }
        }
        return Buyer.reconstitute(
                entity.getId(),
                user,
                toAddress(entity.getMainAddress()),
                additionalAddresses,
                entity.getCommercialStatusCode());
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
