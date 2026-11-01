# Catálogos (Enums) y Value Objects

> Paquete de catálogos: `application.domain.enums`.
> Paquete de Value Objects: `application.domain.valueobjects`.
> Todos los catálogos extienden `DomainCatalog` (D-02) e implementan
> `values()` y `fromCode(String)`. Todos los Value Objects son inmutables,
> validan en su factoría y definen `equals`/`hashCode` por valor.

## 1. `DomainCatalog` (clase base)

| Elemento | Contrato |
|---|---|
| Atributos | `code: String`, `description: String` (ambos no nulos) |
| Métodos | `getCode()`, `getDescription()`, `toString()` devuelve `code` |
| Igualdad | por `code` + clase concreta |
| Búsqueda | cada subclase expone `fromCode(String)` (trim + `toUpperCase`, vacío o desconocido ⇒ `InvalidDataException`) y `values()` |

## 2. Catálogos existentes (ajustados)

| Catálogo | Valores | Uso |
|---|---|---|
| `UserRole` | `BUYER`, `SELLER`, `LOGISTIC_OPERATOR`, `ADMIN`, `SUPERVISOR` | Rol único por usuario (RG-02) |
| `UserStatus` | `ACTIVE`, `BLOCKED`, `INACTIVE` | `INACTIVE` = baja lógica |
| `CommercialStatus` | `ELIGIBLE`, `RESTRICTED`, `SUSPENDED` | Estado comercial del comprador (D-04) |
| `ProductType` | `PHYSICAL`, `DIGITAL` | Inventario/envío solo para físicos |
| `ProductStatus` | `PUBLISHED`, `SUSPENDED`, `DISCONTINUED` | Ciclo de vida del catálogo |
| `OrderStatus` | `CART`, `PENDING_PAYMENT`, `PAID`, `DISPATCHED`, `DELIVERED`, `FINALIZED`, `CANCELLED` | Máquina de estados D-03/D-08 |
| `PaymentStatus` | `PENDING`, `CONFIRMED`, `FAILED`, `REFUNDED` | Estado del pago (`REFUNDED` reservado, A-07) |
| `ShipmentStatus` | `PENDING`, `IN_TRANSIT`, `DELIVERED` | Ciclo del envío |
| `ReturnStatus` | `REQUESTED`, `APPROVED`, `REJECTED`, `COMPLETED` | Ciclo de la devolución |

> Cambio obligatorio en `CommercialStatus`: eliminar `ACTIVE`, crear `ELIGIBLE`
> y añadir `SUSPENDED`. Añadir `values()` a todos los catálogos (hoy solo lo
> tiene `UserRole`).

## 3. Catálogos nuevos (D-06)

| Catálogo | Valores | Uso |
|---|---|---|
| `WarehouseType` | `MARKETPLACE`, `SELLER` | Propiedad de la bodega |
| `MovementType` | `INFLOW`, `RESERVATION`, `SALE`, `ADJUSTMENT`, `RETURN` | Tipo de movimiento de inventario |
| `InventoryCondition` | `GOOD`, `DAMAGED` | Reemplaza el booleano `damaged` |
| `InvoiceStatus` | `ISSUED`, `VOIDED` | Reemplaza el booleano `voided` |
| `RefundStatus` | `PENDING`, `PROCESSED` | Reemplaza el booleano `processed` |
| `VariantType` | `COLOR`, `SIZE`, `MODEL`, `OTHER` | Tipo de variante de producto |
| `AuditEntityType` | `USER`, `BUYER`, `SELLER`, `WAREHOUSE`, `PRODUCT`, `INVENTORY`, `INVENTORY_MOVEMENT`, `CART`, `ORDER`, `INVOICE`, `SHIPMENT`, `RETURN`, `REFUND` | Campo `entityType` de `AuditLog` (A-02) |
| `AuditAction` | `USER_CREATED`, `USER_BLOCKED`, `USER_ACTIVATED`, `USER_DEACTIVATED`, `SELLER_REGISTERED`, `SELLER_TAX_ID_UPDATED`, `WAREHOUSE_CREATED`, `WAREHOUSE_UPDATED`, `WAREHOUSE_ASSIGNED`, `PRODUCT_REGISTERED`, `PRODUCT_UPDATED`, `PRODUCT_PUBLISHED`, `PRODUCT_SUSPENDED`, `PRODUCT_DISCONTINUED`, `INVENTORY_MOVEMENT`, `ORDER_CREATED`, `ORDER_STATUS_CHANGED`, `INVOICE_ISSUED`, `INVOICE_VOIDED`, `SHIPMENT_CREATED`, `SHIPMENT_STARTED`, `SHIPMENT_DELIVERED`, `RETURN_REQUESTED`, `RETURN_APPROVED`, `RETURN_REJECTED`, `RETURN_COMPLETED`, `REFUND_CREATED`, `REFUND_PROCESSED` | Acciones auditables (D-14) |

## 4. Reglas comunes de los catálogos

- `fromCode` acepta el código exacto en cualquier capitalización y hace `trim`.
- `fromCode(null)`, `fromCode("")` o código desconocido ⇒
  `InvalidDataException("Invalid <Catalog> code: ...")`.
- `values()` devuelve una copia del arreglo de singletons.
- No se usan en `switch` ni con `@Enumerated`; la persistencia usa
  `AttributeConverter` (JPA) y conversión explícita en Mongo/REST.
- Los códigos son estables: cambiarlos rompe datos persistidos.

## 5. Value Objects

### `Money`

| Elemento | Contrato |
|---|---|
| Atributos | `amount: BigDecimal` (escala 2, `HALF_UP`), `currency: Currency` |
| Factorías | `of(BigDecimal, Currency)`, `of(BigDecimal)` (COP), `cop(BigDecimal)` |
| Validación | `amount != null`, no negativo; `currency != null` |
| Operaciones | `add(Money)`, `subtract(Money)`, `multiply(int)`; misma moneda o `InvalidDataException`; resta no negativa |
| Consultas | `isPositive()`, `isZero()`, `isGreaterThan(Money)`, `isLessThanOrEqualTo(Money)` |
| Igualdad | por `amount.compareTo(...) == 0` y `currency` |

### `Address`

| Atributo | Tipo | Validación |
|---|---|---|
| `street` | `String` | no vacío |
| `city` | `String` | no vacío |
| `region` | `String` | no vacío |
| `postalCode` | `String` | opcional |
| `country` | `String` | no vacío |

`of(...)` normaliza `trim`. Dirección inválida ⇒ `InvalidAddressException`
(los servicios traducen la validación del VO cuando corresponda).

### `Email`

- Normaliza `trim` + minúsculas.
- Formato: `^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$`.
- Inválido ⇒ `InvalidDataException("Invalid email: ...")`.
- Igualdad por valor normalizado.

### `FullName`

- `trim`, no vacío, longitud 2–120.
- Inválido ⇒ `InvalidDataException("Full name is required")` o
  `"Full name length must be between 2 and 120"`.

### `IdentityDocument`

- `trim` + mayúsculas.
- Formato: `^[A-Z0-9-]{5,30}$`.
- Inválido ⇒ `InvalidDataException("Invalid identity document")`.

### `TaxId`

- `trim`; formato simple documentado: `^[0-9]{9,15}(-[0-9])?$`
  (9–15 dígitos, con dígito de verificación opcional precedido de guion).
- Inválido ⇒ `InvalidDataException("Invalid tax id")`.

### `ProductVariant`

| Atributo | Tipo | Validación |
|---|---|---|
| `type` | `VariantType` | no nulo |
| `value` | `String` | `trim` no vacío, ≤ 50 caracteres |

Igualdad por `type` + `value` (ignorando mayúsculas en `value`).

### `PasswordHash`

- Envoltorio opaco del hash ya calculado por `PasswordServicePort`.
- No vacío; **no** valida el algoritmo (BCrypt se decide en infraestructura).
- `toString()` devuelve `"***"` para no filtrar el hash en logs.

### `StockAllocation`

| Atributo | Tipo | Validación |
|---|---|---|
| `productId` | `String` | no vacío |
| `warehouseId` | `String` | no vacío |
| `quantity` | `int` | > 0 |

Inmutable; igualdad por los tres campos. Representa la reserva de una línea de
pedido en una bodega concreta (D-07/D-08).

## 6. Relación VO ↔ entidades

| VO | Entidades que lo usan |
|---|---|
| `Money` | `Product`, `CartItem`, `Order`, `Invoice`, `Refund`, reportes |
| `Address` | `Buyer`, `Warehouse`, `Order`, `Shipment` |
| `Email`, `FullName`, `IdentityDocument`, `PasswordHash` | `User` |
| `TaxId` | `Seller` |
| `ProductVariant` | `Product` |
| `StockAllocation` | `Order` |

## 7. Prohibiciones explícitas (R-04/R-05)

- Ningún atributo de estado, rol, tipo, condición o movimiento se modela como
  `String` abierto.
- Ningún importe usa `double`, `float` ni `Double`; solo `BigDecimal` dentro de
  `Money`.
- `String` se reserva para texto libre real: `User`/`Product` names,
  `description`, `reason`, `trackingNumber`, `invoice.number`, `AuditLog.details`,
  `entityId`.
