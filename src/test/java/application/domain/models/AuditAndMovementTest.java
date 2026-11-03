package application.domain.models;

import static application.domain.DomainFixtures.admin;
import static application.domain.DomainFixtures.operator;
import static application.domain.DomainFixtures.publishedPhysicalProduct;
import static application.domain.DomainFixtures.seller;
import static application.domain.DomainFixtures.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import application.domain.enums.AuditAction;
import application.domain.enums.AuditEntityType;
import application.domain.enums.MovementType;
import application.domain.enums.UserRole;
import application.domain.exceptions.common.InvalidDataException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class AuditAndMovementTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    @Test
    void auditLogCapturesActorRoleAndDetails() {
        User actor = user(UserRole.ADMIN);
        AuditLog log = AuditLog.create(AuditAction.USER_BLOCKED, AuditEntityType.USER,
                "entity-1", actor, NOW, "blocked");
        assertThat(log.getPerformedBy()).isEqualTo(actor.getId());
        assertThat(log.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(log.getDetails()).isEqualTo("blocked");
    }

    @Test
    void inventoryMovementRejectsZeroAndNegativeNonAdjustments() {
        Product product = publishedPhysicalProduct(seller(admin()), operator());
        Inventory inventory = Inventory.create(product, product.getSeller().getWarehouses().get(0), 5);
        assertThatThrownBy(() -> InventoryMovement.create(inventory, MovementType.INFLOW, 0, operator(), NOW))
                .isInstanceOf(InvalidDataException.class);
        assertThatThrownBy(() -> InventoryMovement.create(inventory, MovementType.INFLOW, -1, operator(), NOW))
                .isInstanceOf(InvalidDataException.class);
        InventoryMovement adjustment = InventoryMovement.create(
                inventory, MovementType.ADJUSTMENT, -1, operator(), NOW);
        assertThat(adjustment.getQuantity()).isEqualTo(-1);
    }
}
