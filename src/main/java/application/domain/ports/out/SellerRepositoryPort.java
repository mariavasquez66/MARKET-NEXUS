package application.domain.ports.out;

import application.domain.models.Seller;
import application.domain.valueobjects.TaxId;
import java.util.List;
import java.util.Optional;

public interface SellerRepositoryPort {
    Seller save(Seller seller);

    Optional<Seller> findById(String id);

    Optional<Seller> findByUserId(String userId);

    List<Seller> findAll();

    boolean existsByTaxId(TaxId taxId);
}
