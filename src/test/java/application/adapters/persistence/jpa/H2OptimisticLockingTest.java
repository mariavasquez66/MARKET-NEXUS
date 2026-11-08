package application.adapters.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import application.adapters.persistence.jpa.entities.InventoryJpaEntity;
import application.adapters.persistence.jpa.repositories.InventoryJpaRepository;
import application.domain.enums.InventoryCondition;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class H2OptimisticLockingTest {

    @Autowired
    private InventoryJpaRepository inventoryRepository;

    @Test
    void simultaneousReservationsOnSameStockRejectOneAndDoNotOversell() throws Exception {
        InventoryJpaEntity seed = inventory(1);
        inventoryRepository.saveAndFlush(seed);

        InventoryJpaEntity firstReservation = inventoryRepository.findById(seed.getId()).orElseThrow();
        InventoryJpaEntity secondReservation = inventoryRepository.findById(seed.getId()).orElseThrow();

        reserve(firstReservation);
        Throwable failure = null;
        try {
            reserve(secondReservation);
        } catch (RuntimeException ex) {
            failure = ex;
        }

        assertThat(failure).isInstanceOf(OptimisticLockingFailureException.class);
        InventoryJpaEntity stored = inventoryRepository.findById(seed.getId()).orElseThrow();
        assertThat(stored.getAvailableQuantity()).isZero();
        assertThat(stored.getReservedQuantity()).isEqualTo(1);
        assertThat(stored.getVersion()).isEqualTo(1L);
    }

    private void reserve(InventoryJpaEntity inventory) {
        inventory.setAvailableQuantity(inventory.getAvailableQuantity() - 1);
        inventory.setReservedQuantity(inventory.getReservedQuantity() + 1);
        inventoryRepository.saveAndFlush(inventory);
    }

    private static InventoryJpaEntity inventory(int available) {
        InventoryJpaEntity entity = new InventoryJpaEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setProductId(UUID.randomUUID().toString());
        entity.setWarehouseId(UUID.randomUUID().toString());
        entity.setAvailableQuantity(available);
        entity.setReservedQuantity(0);
        entity.setConditionCode(InventoryCondition.GOOD);
        return entity;
    }
}
