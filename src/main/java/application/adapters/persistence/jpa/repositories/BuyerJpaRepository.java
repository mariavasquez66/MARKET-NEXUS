package application.adapters.persistence.jpa.repositories;

import application.adapters.persistence.jpa.entities.BuyerJpaEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BuyerJpaRepository extends JpaRepository<BuyerJpaEntity, String> {

    Optional<BuyerJpaEntity> findByUserId(String userId);

    @Query("select distinct b from BuyerJpaEntity b "
            + "left join fetch b.additionalAddresses where b.id in :ids")
    List<BuyerJpaEntity> findAllByIdInWithAdditionalAddresses(@Param("ids") Collection<String> ids);
}
