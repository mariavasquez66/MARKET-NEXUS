package application.adapters.persistence.jpa.mappers.converters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import application.domain.enums.AuditAction;
import application.domain.enums.AuditEntityType;
import application.domain.enums.CommercialStatus;
import application.domain.enums.DomainCatalog;
import application.domain.enums.InventoryCondition;
import application.domain.enums.InvoiceStatus;
import application.domain.enums.MovementType;
import application.domain.enums.OrderStatus;
import application.domain.enums.PaymentStatus;
import application.domain.enums.ProductStatus;
import application.domain.enums.ProductType;
import application.domain.enums.RefundStatus;
import application.domain.enums.ReturnStatus;
import application.domain.enums.ShipmentStatus;
import application.domain.enums.UserRole;
import application.domain.enums.UserStatus;
import application.domain.enums.VariantType;
import application.domain.enums.WarehouseType;
import application.domain.exceptions.common.InvalidDataException;
import jakarta.persistence.AttributeConverter;
import org.junit.jupiter.api.Test;

class CatalogConvertersTest {

    @Test
    void auditActionConverterHandlesCatalogNullAndInvalidCodes() {
        assertConverter(new AuditActionConverter(), AuditAction.USER_CREATED);
    }

    @Test
    void auditEntityTypeConverterHandlesCatalogNullAndInvalidCodes() {
        assertConverter(new AuditEntityTypeConverter(), AuditEntityType.ORDER);
    }

    @Test
    void commercialStatusConverterHandlesCatalogNullAndInvalidCodes() {
        assertConverter(new CommercialStatusConverter(), CommercialStatus.RESTRICTED);
    }

    @Test
    void inventoryConditionConverterHandlesCatalogNullAndInvalidCodes() {
        assertConverter(new InventoryConditionConverter(), InventoryCondition.DAMAGED);
    }

    @Test
    void invoiceStatusConverterHandlesCatalogNullAndInvalidCodes() {
        assertConverter(new InvoiceStatusConverter(), InvoiceStatus.ISSUED);
    }

    @Test
    void movementTypeConverterHandlesCatalogNullAndInvalidCodes() {
        assertConverter(new MovementTypeConverter(), MovementType.INFLOW);
    }

    @Test
    void orderStatusConverterHandlesCatalogNullAndInvalidCodes() {
        assertConverter(new OrderStatusConverter(), OrderStatus.PAID);
    }

    @Test
    void paymentStatusConverterHandlesCatalogNullAndInvalidCodes() {
        assertConverter(new PaymentStatusConverter(), PaymentStatus.CONFIRMED);
    }

    @Test
    void productStatusConverterHandlesCatalogNullAndInvalidCodes() {
        assertConverter(new ProductStatusConverter(), ProductStatus.PUBLISHED);
    }

    @Test
    void productTypeConverterHandlesCatalogNullAndInvalidCodes() {
        assertConverter(new ProductTypeConverter(), ProductType.PHYSICAL);
    }

    @Test
    void refundStatusConverterHandlesCatalogNullAndInvalidCodes() {
        assertConverter(new RefundStatusConverter(), RefundStatus.PROCESSED);
    }

    @Test
    void returnStatusConverterHandlesCatalogNullAndInvalidCodes() {
        assertConverter(new ReturnStatusConverter(), ReturnStatus.APPROVED);
    }

    @Test
    void shipmentStatusConverterHandlesCatalogNullAndInvalidCodes() {
        assertConverter(new ShipmentStatusConverter(), ShipmentStatus.IN_TRANSIT);
    }

    @Test
    void userRoleConverterHandlesCatalogNullAndInvalidCodes() {
        assertConverter(new UserRoleConverter(), UserRole.ADMIN);
    }

    @Test
    void userStatusConverterHandlesCatalogNullAndInvalidCodes() {
        assertConverter(new UserStatusConverter(), UserStatus.BLOCKED);
    }

    @Test
    void variantTypeConverterHandlesCatalogNullAndInvalidCodes() {
        assertConverter(new VariantTypeConverter(), VariantType.COLOR);
    }

    @Test
    void warehouseTypeConverterHandlesCatalogNullAndInvalidCodes() {
        assertConverter(new WarehouseTypeConverter(), WarehouseType.MARKETPLACE);
    }

    private <T extends DomainCatalog> void assertConverter(
            AttributeConverter<T, String> converter, T validValue) {
        assertThat(converter.convertToDatabaseColumn(validValue)).isEqualTo(validValue.getCode());
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
        assertThat(converter.convertToEntityAttribute(validValue.getCode())).isEqualTo(validValue);
        assertThatThrownBy(() -> converter.convertToEntityAttribute("__INVALID__"))
                .isInstanceOf(InvalidDataException.class);
    }
}
