package application.domain.services;

import static application.domain.DomainFixtures.admin;
import static application.domain.DomainFixtures.operator;
import static application.domain.DomainFixtures.publishedPhysicalProduct;
import static application.domain.DomainFixtures.seller;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import application.domain.exceptions.common.UnauthorizedOperationException;
import application.domain.models.Inventory;
import application.domain.models.Product;
import application.domain.models.Seller;
import application.domain.ports.out.InventoryRepositoryPort;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InventoryQueryServiceTest {

    @Mock InventoryRepositoryPort inventoryRepository;
    @InjectMocks InventoryQueryService service;

    @Test
    void sellerCanOnlyReadOwnInventory() {
        Seller owner = seller(admin());
        Product product = publishedPhysicalProduct(owner, operator());
        Inventory inventory = Inventory.create(product, product.getSeller().getWarehouses().get(0), 5);
        when(inventoryRepository.findByProductIdAndWarehouseId(
                product.getId(), inventory.getWarehouse().getId())).thenReturn(Optional.of(inventory));

        assertThat(service.getAvailable(owner.getUser(), product.getId(),
                inventory.getWarehouse().getId())).isSameAs(inventory);
        assertThatThrownBy(() -> service.getAvailable(seller(admin()).getUser(), product.getId(),
                inventory.getWarehouse().getId()))
                .isInstanceOf(UnauthorizedOperationException.class);
    }

    @Test
    void listMyInventoryFiltersByAuthenticatedSeller() {
        Seller owner = seller(admin());
        Product own = publishedPhysicalProduct(owner, operator());
        Product foreign = publishedPhysicalProduct(seller(admin()), operator());
        Inventory ownInventory = Inventory.create(own, own.getSeller().getWarehouses().get(0), 5);
        Inventory foreignInventory = Inventory.create(
                foreign, foreign.getSeller().getWarehouses().get(0), 5);
        when(inventoryRepository.findAll()).thenReturn(List.of(ownInventory, foreignInventory));

        assertThat(service.listMyInventory(owner.getUser())).containsExactly(ownInventory);
    }
}
