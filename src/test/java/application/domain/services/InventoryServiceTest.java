package application.domain.services;

import static application.domain.DomainFixtures.admin;
import static application.domain.DomainFixtures.buyer;
import static application.domain.DomainFixtures.operator;
import static application.domain.DomainFixtures.publishedDigitalProduct;
import static application.domain.DomainFixtures.publishedPhysicalProduct;
import static application.domain.DomainFixtures.seller;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import application.domain.exceptions.common.DuplicateEntityException;
import application.domain.exceptions.common.InsufficientPermissionException;
import application.domain.exceptions.inventory.InsufficientStockException;
import application.domain.exceptions.product.InvalidProductTypeException;
import application.domain.models.Inventory;
import application.domain.models.Product;
import application.domain.models.Seller;
import application.domain.models.Warehouse;
import application.domain.ports.out.AuditPort;
import application.domain.ports.out.ClockPort;
import application.domain.ports.out.InventoryMovementRepositoryPort;
import application.domain.ports.out.InventoryRepositoryPort;
import application.domain.ports.out.ProductRepositoryPort;
import application.domain.ports.out.WarehouseRepositoryPort;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class InventoryServiceTest {
    @Mock InventoryRepositoryPort inventoryRepository;
    @Mock InventoryMovementRepositoryPort movementRepository;
    @Mock ProductRepositoryPort productRepository;
    @Mock WarehouseRepositoryPort warehouseRepository;
    @Mock AuditPort auditPort;
    @Mock ClockPort clockPort;

    private InventoryService service;

    @BeforeEach
    void setUp() {
        service = new InventoryService(inventoryRepository, movementRepository, productRepository,
                warehouseRepository, auditPort, clockPort);
        when(clockPort.now()).thenReturn(Instant.parse("2026-01-01T00:00:00Z"));
        when(inventoryRepository.save(any(Inventory.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void initialStockRequiresPhysicalProductAndSellerOwnership() {
        Product digital = publishedDigitalProduct(seller(admin()));
        when(productRepository.findById(digital.getId())).thenReturn(Optional.of(digital));
        assertThatThrownBy(() -> service.registerInitialStock(digital.getSeller().getUser(),
                digital.getId(), "w", 1)).isInstanceOf(InvalidProductTypeException.class);

        Seller owner = seller(admin());
        Product physical = publishedPhysicalProduct(owner, operator());
        Warehouse warehouse = owner.getWarehouses().get(0);
        when(productRepository.findById(physical.getId())).thenReturn(Optional.of(physical));
        when(warehouseRepository.findById(warehouse.getId())).thenReturn(Optional.of(warehouse));
        assertThatThrownBy(() -> service.registerInitialStock(seller(admin()).getUser(),
                physical.getId(), warehouse.getId(), 1))
                .isInstanceOf(application.domain.exceptions.common.UnauthorizedOperationException.class);
    }

    @Test
    void duplicateInventoryIsRejected() {
        Seller owner = seller(admin());
        Product physical = publishedPhysicalProduct(owner, operator());
        Warehouse warehouse = owner.getWarehouses().get(0);
        when(productRepository.findById(physical.getId())).thenReturn(Optional.of(physical));
        when(warehouseRepository.findById(warehouse.getId())).thenReturn(Optional.of(warehouse));
        when(inventoryRepository.findByProductIdAndWarehouseId(physical.getId(), warehouse.getId()))
                .thenReturn(Optional.of(Inventory.create(physical, warehouse, 1)));
        assertThatThrownBy(() -> service.registerInitialStock(owner.getUser(), physical.getId(),
                warehouse.getId(), 1)).isInstanceOf(DuplicateEntityException.class);
    }

    @Test
    void initialStockCreatesMovementAndAudit() {
        Seller owner = seller(admin());
        Product physical = publishedPhysicalProduct(owner, operator());
        Warehouse warehouse = owner.getWarehouses().get(0);
        when(productRepository.findById(physical.getId())).thenReturn(Optional.of(physical));
        when(warehouseRepository.findById(warehouse.getId())).thenReturn(Optional.of(warehouse));
        when(inventoryRepository.findByProductIdAndWarehouseId(physical.getId(), warehouse.getId()))
                .thenReturn(Optional.empty());
        Inventory inventory = service.registerInitialStock(owner.getUser(), physical.getId(),
                warehouse.getId(), 5);
        assertThat(inventory.getAvailableQuantity()).isEqualTo(5);
        verify(movementRepository).save(any());
        verify(auditPort).record(any());
    }

    @Test
    void reserveFailsWhenStockIsInsufficient() {
        Seller owner = seller(admin());
        Product physical = publishedPhysicalProduct(owner, operator());
        Warehouse warehouse = owner.getWarehouses().get(0);
        Inventory inventory = Inventory.create(physical, warehouse, 1);
        when(inventoryRepository.findByProductIdAndWarehouseId(physical.getId(), warehouse.getId()))
                .thenReturn(Optional.of(inventory));
        assertThatThrownBy(() -> service.reserve(buyer().getUser(), physical.getId(),
                warehouse.getId(), 2)).isInstanceOf(InsufficientStockException.class);
    }

    @Test
    void onlyLogisticOperatorCanAdjust() {
        assertThatThrownBy(() -> service.adjust(buyer().getUser(), "p", "w", 1))
                .isInstanceOf(InsufficientPermissionException.class);
    }
}
