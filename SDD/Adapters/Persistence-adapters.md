# Adaptadores de persistencia

> Paquetes:
> `application.adapters.persistence.jpa.{entities,mappers,repositories,adapters}` y
> `application.adapters.persistence.mongodb.{documents,mappers,repositories,adapters}`.
>
> Regla de oro: el dominio no conoce estas clases. Cada adaptador implementa un
> puerto de salida y traduce con un mapper bidireccional.

## 1. JPA (MySQL `nexusmarket_db`)

### 1.1. Entidades `*JpaEntity`

| Dominio | Entidad JPA | Tabla | Restricciones |
|---|---|---|---|
| `User` | `UserJpaEntity` | `users` | `email` UNIQUE, `identity_document` UNIQUE |
| `Buyer` | `BuyerJpaEntity` | `buyers` | `user_id` UNIQUE FK `users` |
| `Seller` | `SellerJpaEntity` | `sellers` | `user_id` UNIQUE FK `users`, `tax_id` UNIQUE |
| `Warehouse` | `WarehouseJpaEntity` | `warehouses` | `owner_seller_id` FK `sellers` (nullable), `responsible_user_id` FK `users` |
| `Product` | `ProductJpaEntity` | `products` | `seller_id` FK `sellers` |
| `Inventory` | `InventoryJpaEntity` | `inventories` | UNIQUE(`product_id`,`warehouse_id`), FKs |
| `InventoryMovement` | `InventoryMovementJpaEntity` | `inventory_movements` | FK `inventory_id`, `performed_by_user_id` |
| `Cart` | `CartJpaEntity` | `carts` | `buyer_id` UNIQUE FK `buyers` |
| `CartItem` | `CartItemJpaEntity` | `cart_items` | FK `cart_id`, `product_id`; UNIQUE(`cart_id`,`product_id`) |
| `Order` | `OrderJpaEntity` | `orders` | `buyer_id` FK `buyers`, `invoice_id` FK `invoices` (nullable) |
| `CartItem` (copia) | `OrderItemJpaEntity` | `order_items` | FK `order_id`, `product_id` |
| `StockAllocation` | `OrderAllocationJpaEntity` | `order_allocations` | FK `order_id`; columnas `product_id`, `warehouse_id`, `quantity` |
| `Invoice` | `InvoiceJpaEntity` | `invoices` | `order_id` UNIQUE FK `orders`, `number` UNIQUE |
| `Shipment` | `ShipmentJpaEntity` | `shipments` | FK `order_id`, `origin_warehouse_id` |
| `Return` | `ReturnJpaEntity` | `returns` | FK `order_id` |
| `Refund` | `RefundJpaEntity` | `refunds` | FK `return_id` |

`AuditLog` **no** se mapea en JPA (vive en Mongo).

### 1.2. Columnas y tipos

- IDs: `VARCHAR(36)`.
- Texto libre: `VARCHAR(255)` salvo `description`, `reason`, `details`
  (`VARCHAR(1000)` o `TEXT`).
- Importes: `DECIMAL(19,2)` + `VARCHAR(3)` para la moneda (`COP`).
- Enums/catálogos: `VARCHAR(40)` con el `code` del `DomainCatalog`.
- Fechas: `DATETIME(6)` ↔ `Instant`.
- Booleanos: no se usan para estados; condición/estado son catálogos.

### 1.3. Converters JPA

Un `AttributeConverter<XxxCatalog, String>` por catálogo, en
`adapters.persistence.jpa.mappers.converters`:

`UserRoleConverter`, `UserStatusConverter`, `CommercialStatusConverter`,
`ProductTypeConverter`, `ProductStatusConverter`, `OrderStatusConverter`,
`PaymentStatusConverter`, `ShipmentStatusConverter`, `ReturnStatusConverter`,
`WarehouseTypeConverter`, `MovementTypeConverter`, `InventoryConditionConverter`,
`InvoiceStatusConverter`, `RefundStatusConverter`, `VariantTypeConverter`,
`AuditActionConverter`, `AuditEntityTypeConverter`.

Cada converter usa `fromCode(...)` al leer y `getCode()` al escribir. Valor
desconocido ⇒ `InvalidDataException` (se propaga como error de integridad).

Value Objects embebidos mediante clases JPA `@Embeddable`
(`MoneyEmbeddable`, `AddressEmbeddable`) o columnas planas convertidas por el
mapper. Los catálogos nunca usan `@Enumerated`.

### 1.4. Repositorios Spring Data

`*JpaRepository extends JpaRepository<XxxJpaEntity, String>` con consultas
derivadas:

- `UserJpaRepository`: `findByEmailValue`, `existsByEmailValue`,
  `existsByIdentityDocumentValue`, `existsByRoleCode`.
- `BuyerJpaRepository`: `findByUserId`.
- `SellerJpaRepository`: `findByUserId`, `existsByTaxIdValue`.
- `WarehouseJpaRepository`: `findByOwnerId`, `existsByOwnerId`.
- `ProductJpaRepository`: `findByStatusCode`, `findBySellerId`.
- `InventoryJpaRepository`: `findByProductIdAndWarehouseId`,
  `findByProductId`, `findByWarehouseId`, `findByWarehouseOwnerId`,
  `existsByProductId`.
- `CartJpaRepository`: `findByBuyerId`, `deleteByBuyerId`.
- `OrderJpaRepository`: `findByBuyerId`, `findByStatus`, `findByBuyerIdAndStatus`,
  `findByPaymentStatusCodeAndCreatedAtBetween`,
  `findDistinctByItemsProductSellerId`.
- `InvoiceJpaRepository`: `findByOrderId`, `existsByOrderId`,
  `findByOrderBuyerId`.
- `ShipmentJpaRepository`: `findByOrderId`,
  `findByOrderIdAndOriginWarehouseId`.
- `ReturnJpaRepository`: `findByOrderId`, `findByStatus`,
  `findByOrderBuyerId`.
- `RefundJpaRepository`: `findByReturnId`,
  `findByReturnOrderBuyerId`.

### 1.5. Mappers `*JpaMapper`

Un mapper por agregado: `UserJpaMapper`, `BuyerJpaMapper`, `SellerJpaMapper`,
`WarehouseJpaMapper`, `ProductJpaMapper`, `InventoryJpaMapper`,
`InventoryMovementJpaMapper`, `CartJpaMapper`, `OrderJpaMapper`,
`InvoiceJpaMapper`, `ShipmentJpaMapper`, `ReturnJpaMapper`, `RefundJpaMapper`.

Contrato:

```java
XxxJpaEntity toEntity(Xxx domain);
Xxx toDomain(XxxJpaEntity entity);
```

- `toDomain` usa `reconstitute(...)` para no regenerar IDs.
- `toEntity` no persiste; solo traduce.
- Las relaciones bidireccionales `Seller` ↔ `Warehouse` se mapean por el lado
  propietario (`Warehouse.owner`); el mapper de `Seller` reconstruye la lista
  desde `WarehouseJpaRepository.findByOwnerId` o desde la colección JPA, sin
  recursión infinita (A-05).
- Los `CartItem`/`OrderItem`/`StockAllocation` se mapean dentro de su agregado.

### 1.6. Adaptadores `*JpaAdapter`

Cada `*JpaAdapter implements <Puerto>` y delega en el repositorio + mapper. Son
`@Component` de Spring y `@Transactional` en las escrituras. Nunca exponen
entidades JPA fuera del paquete de persistencia.

## 2. MongoDB (auditoría)

Base: `nexus_audit_db`; colección: `audit_logs` (perfil `local` y `docker`).

### 2.1. `AuditLogDocument`

```java
@Document(collection = "audit_logs")
public class AuditLogDocument {
    @Id private String id;
    private String actionCode;
    private String entityTypeCode;
    private String entityId;
    private String performedBy;
    private String roleCode;
    private Instant occurredAt;
    private String details;
}
```

### 2.2. Clases

| Clase | Responsabilidad |
|---|---|
| `AuditLogMongoMapper` | `AuditLog ↔ AuditLogDocument` (sin pérdida) |
| `AuditLogMongoRepository extends MongoRepository<AuditLogDocument, String>` | persistencia; consultas por `actionCode` y rango de `occurredAt` |
| `AuditLogMongoAdapter implements AuditPort` | `record`, `find(action, from, to)`, `findAll()` |

La colección se crea en el primer `record(...)`; no requiere migración.

## 3. Pruebas previstas (Fase 4 y 7)

- `UserJpaMapperTest`, `ProductJpaMapperTest`, `OrderJpaMapperTest`,
  `InventoryJpaMapperTest`, … (ida y vuelta sin pérdida).
- `*JpaAdapterTest` con repositorio mockeado (verifica mapeo y llamada).
- `AuditLogMongoMapperTest`.
- No se requieren MySQL/Mongo reales para los tests unitarios (D-17).
