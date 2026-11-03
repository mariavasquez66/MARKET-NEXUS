package application.adapters.persistence.jpa.repositories;

import application.adapters.persistence.jpa.entities.CartItemJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartItemJpaRepository extends JpaRepository<CartItemJpaEntity, String> {

    List<CartItemJpaEntity> findByCartId(String cartId);

    void deleteByCartId(String cartId);
}
