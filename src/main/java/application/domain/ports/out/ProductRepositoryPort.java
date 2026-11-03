package application.domain.ports.out;

import application.domain.models.Product;
import java.util.List;
import java.util.Optional;

public interface ProductRepositoryPort {
    Product save(Product product);

    Optional<Product> findById(String id);

    List<Product> findAll();

    List<Product> findPublished();

    List<Product> findBySellerId(String sellerId);
}
