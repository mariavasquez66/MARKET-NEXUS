package application.adapters.persistence.jpa.repositories;

import application.adapters.persistence.jpa.entities.CartJpaEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartJpaRepository extends JpaRepository<CartJpaEntity, String> {

    Optional<CartJpaEntity> findByBuyerId(String buyerId);

    void deleteByBuyerId(String buyerId);
}
