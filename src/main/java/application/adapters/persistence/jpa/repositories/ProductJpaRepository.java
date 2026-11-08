package application.adapters.persistence.jpa.repositories;

import application.adapters.persistence.jpa.entities.ProductJpaEntity;
import application.domain.enums.ProductStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductJpaRepository extends JpaRepository<ProductJpaEntity, String> {

    List<ProductJpaEntity> findByStatusCode(ProductStatus statusCode);

    List<ProductJpaEntity> findBySellerId(String sellerId);
}
