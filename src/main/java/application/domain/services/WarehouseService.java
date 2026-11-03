package application.domain.services;

import application.domain.enums.AuditAction;
import application.domain.enums.AuditEntityType;
import application.domain.enums.UserRole;
import application.domain.enums.WarehouseType;
import application.domain.exceptions.common.UnauthorizedOperationException;
import application.domain.exceptions.seller.SellerNotFoundException;
import application.domain.exceptions.user.InvalidRoleException;
import application.domain.exceptions.user.UserNotFoundException;
import application.domain.exceptions.warehouse.InvalidWarehouseTypeException;
import application.domain.exceptions.warehouse.WarehouseNotFoundException;
import application.domain.models.Seller;
import application.domain.models.User;
import application.domain.models.Warehouse;
import application.domain.ports.out.AuditPort;
import application.domain.ports.out.ClockPort;
import application.domain.ports.out.SellerRepositoryPort;
import application.domain.ports.out.UserRepositoryPort;
import application.domain.ports.out.WarehouseRepositoryPort;
import application.domain.valueobjects.Address;
import java.util.List;
import java.util.Objects;

public class WarehouseService {
    private final WarehouseRepositoryPort warehouseRepository;
    private final SellerRepositoryPort sellerRepository;
    private final UserRepositoryPort userRepository;
    private final AuditPort auditPort;
    private final ClockPort clockPort;

    public WarehouseService(WarehouseRepositoryPort warehouseRepository,
                            SellerRepositoryPort sellerRepository,
                            UserRepositoryPort userRepository,
                            AuditPort auditPort,
                            ClockPort clockPort) {
        this.warehouseRepository = Objects.requireNonNull(warehouseRepository);
        this.sellerRepository = Objects.requireNonNull(sellerRepository);
        this.userRepository = Objects.requireNonNull(userRepository);
        this.auditPort = Objects.requireNonNull(auditPort);
        this.clockPort = Objects.requireNonNull(clockPort);
    }

    public Warehouse createWarehouse(User actor,
                                     String name,
                                     Address location,
                                     WarehouseType type,
                                     String ownerSellerId,
                                     String responsibleUserId) {
        ServiceSupport.requireRole(actor, UserRole.ADMIN);
        Objects.requireNonNull(type, "type cannot be null");
        User responsible = userRepository.findById(responsibleUserId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + responsibleUserId));
        if (!responsible.hasRole(UserRole.ADMIN) && !responsible.hasRole(UserRole.LOGISTIC_OPERATOR)) {
            throw new InvalidRoleException("Warehouse responsible must be ADMIN or LOGISTIC_OPERATOR");
        }
        Warehouse warehouse;
        if (type.equals(WarehouseType.SELLER)) {
            if (ownerSellerId == null || ownerSellerId.isBlank()) {
                throw new InvalidWarehouseTypeException("Seller warehouse requires an owner");
            }
            Seller owner = sellerRepository.findById(ownerSellerId)
                    .orElseThrow(() -> new SellerNotFoundException("Seller not found: " + ownerSellerId));
            warehouse = Warehouse.createSellerWarehouse(name, location, owner, responsible);
        } else if (type.equals(WarehouseType.MARKETPLACE)) {
            if (ownerSellerId != null && !ownerSellerId.isBlank()) {
                throw new InvalidWarehouseTypeException("Marketplace warehouse cannot have an owner");
            }
            warehouse = Warehouse.createMarketplace(name, location, responsible);
        } else {
            throw new InvalidWarehouseTypeException("Unsupported warehouse type");
        }
        Warehouse saved = warehouseRepository.save(warehouse);
        ServiceSupport.audit(auditPort, clockPort, AuditAction.WAREHOUSE_CREATED,
                AuditEntityType.WAREHOUSE, saved.getId(), actor, "Warehouse created");
        return saved;
    }

    public Warehouse updateWarehouse(User actor,
                                     String warehouseId,
                                     String name,
                                     Address location,
                                     String responsibleUserId) {
        ServiceSupport.requireRole(actor, UserRole.ADMIN);
        Warehouse warehouse = requireWarehouse(warehouseId);
        User responsible = userRepository.findById(responsibleUserId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + responsibleUserId));
        if (!responsible.hasRole(UserRole.ADMIN) && !responsible.hasRole(UserRole.LOGISTIC_OPERATOR)) {
            throw new InvalidRoleException("Warehouse responsible must be ADMIN or LOGISTIC_OPERATOR");
        }
        warehouse.changeName(name);
        warehouse.changeLocation(location);
        warehouse.assignResponsible(responsible);
        Warehouse saved = warehouseRepository.save(warehouse);
        ServiceSupport.audit(auditPort, clockPort, AuditAction.WAREHOUSE_UPDATED,
                AuditEntityType.WAREHOUSE, saved.getId(), actor, "Warehouse updated");
        return saved;
    }

    public List<Warehouse> listWarehouses(User actor) {
        ServiceSupport.requireAnyRole(actor, UserRole.ADMIN, UserRole.SUPERVISOR,
                UserRole.LOGISTIC_OPERATOR, UserRole.SELLER);
        if (actor.hasRole(UserRole.SELLER)) {
            Seller seller = sellerRepository.findByUserId(actor.getId())
                    .orElseThrow(() -> new SellerNotFoundException("Seller not found for current user"));
            return warehouseRepository.findByOwnerSellerId(seller.getId());
        }
        return warehouseRepository.findAll();
    }

    public List<Warehouse> listBySeller(User actor, String sellerId) {
        ServiceSupport.requireAnyRole(actor, UserRole.ADMIN, UserRole.SUPERVISOR, UserRole.SELLER);
        Seller seller = sellerRepository.findById(sellerId)
                .orElseThrow(() -> new SellerNotFoundException("Seller not found: " + sellerId));
        if (!actor.hasRole(UserRole.ADMIN) && !actor.hasRole(UserRole.SUPERVISOR)
                && !seller.getUser().getId().equals(actor.getId())) {
            throw new UnauthorizedOperationException("Cannot access another seller warehouses");
        }
        return warehouseRepository.findByOwnerSellerId(sellerId);
    }

    public Warehouse getWarehouse(User actor, String warehouseId) {
        ServiceSupport.requireAnyRole(actor, UserRole.ADMIN, UserRole.SUPERVISOR,
                UserRole.LOGISTIC_OPERATOR, UserRole.SELLER);
        Warehouse warehouse = requireWarehouse(warehouseId);
        if (actor.hasRole(UserRole.SELLER)) {
            if (warehouse.getOwner() == null
                    || !warehouse.getOwner().getUser().getId().equals(actor.getId())) {
                throw new UnauthorizedOperationException("Cannot access another seller warehouse");
            }
        }
        return warehouse;
    }

    private Warehouse requireWarehouse(String warehouseId) {
        return warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new WarehouseNotFoundException("Warehouse not found: " + warehouseId));
    }
}
