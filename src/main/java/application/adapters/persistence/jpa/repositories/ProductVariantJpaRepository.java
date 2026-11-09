package application.adapters.persistence.jpa.repositories;

import application.adapters.persistence.jpa.entities.ProductVariantJpaEntity;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductVariantJpaRepository extends JpaRepository<ProductVariantJpaEntity, String> {

    List<ProductVariantJpaEntity> findByProductId(String productId);

    List<ProductVariantJpaEntity> findByProductIdIn(Collection<String> productIds);
}
