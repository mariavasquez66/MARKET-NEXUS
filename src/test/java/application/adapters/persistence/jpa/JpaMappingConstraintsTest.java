package application.adapters.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import application.adapters.persistence.jpa.entities.BuyerJpaEntity;
import application.adapters.persistence.jpa.entities.CartItemJpaEntity;
import application.adapters.persistence.jpa.entities.CartJpaEntity;
import application.adapters.persistence.jpa.entities.InventoryJpaEntity;
import application.adapters.persistence.jpa.entities.InventoryMovementJpaEntity;
import application.adapters.persistence.jpa.entities.InvoiceJpaEntity;
import application.adapters.persistence.jpa.entities.OrderAllocationJpaEntity;
import application.adapters.persistence.jpa.entities.OrderItemJpaEntity;
import application.adapters.persistence.jpa.entities.OrderJpaEntity;
import application.adapters.persistence.jpa.entities.ProductJpaEntity;
import application.adapters.persistence.jpa.entities.ProductVariantJpaEntity;
import application.adapters.persistence.jpa.entities.RefundJpaEntity;
import application.adapters.persistence.jpa.entities.ReturnJpaEntity;
import application.adapters.persistence.jpa.entities.SellerJpaEntity;
import application.adapters.persistence.jpa.entities.ShipmentJpaEntity;
import application.adapters.persistence.jpa.entities.UserJpaEntity;
import application.adapters.persistence.jpa.entities.WarehouseJpaEntity;
import application.domain.enums.DomainCatalog;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class JpaMappingConstraintsTest {

    private static final List<Class<?>> ENTITY_TYPES = List.of(
            UserJpaEntity.class,
            BuyerJpaEntity.class,
            SellerJpaEntity.class,
            WarehouseJpaEntity.class,
            ProductJpaEntity.class,
            ProductVariantJpaEntity.class,
            InventoryJpaEntity.class,
            InventoryMovementJpaEntity.class,
            CartJpaEntity.class,
            CartItemJpaEntity.class,
            OrderJpaEntity.class,
            OrderItemJpaEntity.class,
            OrderAllocationJpaEntity.class,
            InvoiceJpaEntity.class,
            ShipmentJpaEntity.class,
            ReturnJpaEntity.class,
            RefundJpaEntity.class);

    @Test
    void userEmailAndIdentityDocumentColumnsAreUnique() {
        assertUniqueColumn(UserJpaEntity.class, "emailValue", "email");
        assertUniqueColumn(UserJpaEntity.class, "identityDocumentValue", "identity_document");
    }

    @Test
    void sellerTaxIdColumnIsUnique() {
        assertUniqueColumn(SellerJpaEntity.class, "taxIdValue", "tax_id");
    }

    @Test
    void invoiceOrderIdColumnIsUnique() {
        assertUniqueColumn(InvoiceJpaEntity.class, "orderId", "order_id");
    }

    @Test
    void inventoryProductAndWarehouseHaveCompositeUniqueConstraint() {
        Table table = InventoryJpaEntity.class.getAnnotation(Table.class);

        assertThat(table).isNotNull();
        Optional<UniqueConstraint> matchingConstraint = Arrays.stream(table.uniqueConstraints())
                .filter(constraint -> Set.of(constraint.columnNames())
                        .equals(Set.of("product_id", "warehouse_id")))
                .findFirst();
        assertThat(matchingConstraint)
                .as("inventories must declare a unique constraint on product_id and warehouse_id")
                .isPresent();
    }

    @Test
    void inventoryConditionColumnAvoidsMysqlReservedWord() {
        Column column = field(InventoryJpaEntity.class, "conditionCode")
                .getAnnotation(Column.class);

        assertThat(column).isNotNull();
        assertThat(column.name()).isEqualTo("condition_code");
    }

    @Test
    void moneyAmountColumnsUsePrecision19AndScale2() {
        assertMoneyColumn(ProductJpaEntity.class, "priceAmount");
        assertMoneyColumn(CartItemJpaEntity.class, "unitPriceAmount");
        assertMoneyColumn(OrderJpaEntity.class, "totalAmount");
        assertMoneyColumn(OrderItemJpaEntity.class, "unitPriceAmount");
        assertMoneyColumn(InvoiceJpaEntity.class, "subtotalAmount");
        assertMoneyColumn(InvoiceJpaEntity.class, "taxesAmount");
        assertMoneyColumn(InvoiceJpaEntity.class, "totalAmount");
        assertMoneyColumn(RefundJpaEntity.class, "amountAmount");

        assertThat(moneyAmountColumns(InventoryMovementJpaEntity.class)).isEmpty();

        for (Class<?> entityType : ENTITY_TYPES) {
            for (Field field : moneyAmountColumns(entityType)) {
                Column column = field.getAnnotation(Column.class);
                assertThat(column.precision())
                        .as("%s.%s precision", entityType.getSimpleName(), field.getName())
                        .isEqualTo(19);
                assertThat(column.scale())
                        .as("%s.%s scale", entityType.getSimpleName(), field.getName())
                        .isEqualTo(2);
            }
        }
    }

    @Test
    void entitiesDoNotUseEnumeratedAndCatalogFieldsUseConvert() {
        int catalogFields = 0;
        for (Class<?> entityType : ENTITY_TYPES) {
            for (Field field : entityType.getDeclaredFields()) {
                assertThat(field.isAnnotationPresent(Enumerated.class))
                        .as("%s.%s must not use @Enumerated",
                                entityType.getSimpleName(), field.getName())
                        .isFalse();
                if (DomainCatalog.class.isAssignableFrom(field.getType())) {
                    assertThat(field.isAnnotationPresent(Convert.class))
                            .as("%s.%s must use @Convert",
                                    entityType.getSimpleName(), field.getName())
                            .isTrue();
                    catalogFields++;
                }
            }
        }
        assertThat(catalogFields).isPositive();
    }

    private static void assertUniqueColumn(Class<?> entityType,
                                           String fieldName,
                                           String expectedColumnName) {
        Column column = field(entityType, fieldName).getAnnotation(Column.class);

        assertThat(column).isNotNull();
        assertThat(column.name()).isEqualTo(expectedColumnName);
        assertThat(column.unique()).isTrue();
    }

    private static void assertMoneyColumn(Class<?> entityType, String fieldName) {
        Column column = field(entityType, fieldName).getAnnotation(Column.class);

        assertThat(column).isNotNull();
        assertThat(column.name()).endsWith("_amount");
        assertThat(column.precision()).isEqualTo(19);
        assertThat(column.scale()).isEqualTo(2);
    }

    private static List<Field> moneyAmountColumns(Class<?> entityType) {
        return Arrays.stream(entityType.getDeclaredFields())
                .filter(field -> {
                    Column column = field.getAnnotation(Column.class);
                    return column != null && column.name().endsWith("_amount");
                })
                .toList();
    }

    private static Field field(Class<?> entityType, String fieldName) {
        try {
            return entityType.getDeclaredField(fieldName);
        } catch (NoSuchFieldException exception) {
            throw new AssertionError(
                    "Missing field " + entityType.getSimpleName() + "." + fieldName, exception);
        }
    }
}
