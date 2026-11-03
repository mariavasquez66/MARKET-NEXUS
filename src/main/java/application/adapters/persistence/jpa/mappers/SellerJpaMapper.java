package application.adapters.persistence.jpa.mappers;

import application.adapters.persistence.jpa.entities.SellerJpaEntity;
import application.domain.models.Seller;
import application.domain.models.User;
import application.domain.models.Warehouse;
import application.domain.valueobjects.TaxId;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SellerJpaMapper {

    public SellerJpaEntity toEntity(Seller seller) {
        SellerJpaEntity entity = new SellerJpaEntity();
        entity.setId(seller.getId());
        entity.setUserId(seller.getUser().getId());
        entity.setTaxIdValue(seller.getTaxId().getValue());
        return entity;
    }

    public Seller toDomain(SellerJpaEntity entity, User user, List<Warehouse> warehouses) {
        return Seller.reconstitute(
                entity.getId(),
                user,
                TaxId.of(entity.getTaxIdValue()),
                warehouses);
    }
}
