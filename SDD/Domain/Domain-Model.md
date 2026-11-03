# Modelo de dominio — NexusMarket

> Paquete: `application.domain.models`. Todas las entidades son POJOs puros
> (sin Spring, JPA, Mongo, Jackson ni HTTP) y con identidad `String` (UUID)
> generada en sus factorías. Los atributos usan Enums/Value Objects, nunca
> `String` para valores acotados.
>
> Convención de reconstrucción: cada entidad expone un método estático
> `reconstitute(...)` usado exclusivamente por los mappers de persistencia. Las
> factorías de negocio (`create`, `registerBy`, `fromCart`, `request`, …) validan
> las invariantes y generan el identificador.

## 1. Mapa de agregados

| Agregado raíz | Entidades internas | Referencias por identidad/objeto |
|---|---|---|
| `User` | — | — |
| `Buyer` | — | `User` |
| `Seller` | — | `User`, `Warehouse` (lista) |
| `Warehouse` | — | `Seller` (propietario), `User` (responsable) |
| `Product` | — | `Seller` |
| `Inventory` | — | `Product`, `Warehouse` |
| `InventoryMovement` | — | `Inventory`, `User` |
| `Cart` | `CartItem` | `Buyer`, `Product` |
| `Order` | `CartItem` (copia), `StockAllocation` (VO) | `Buyer`, `Invoice`, productos |
| `Invoice` | — | `Order` |
| `Shipment` | — | `Order`, `Warehouse` |
| `Return` | — | `Order` |
| `Refund` | — | `Return` |
| `AuditLog` | — | — |

Modelos de solo lectura: `AuthResult` y los reportes
(`SalesReport`, `OrderSummary`, `InventorySummary`, `ReturnSummary`,
`UserSummary`).

## 2. Dominio 1 — Administración de Usuarios

### `User`

| Atributo | Tipo | Obligatorio | Notas |
|---|---|---|---|
| `id` | `String` | sí | UUID generado en `create` |
| `fullName` | `FullName` | sí | no vacío, 2–120 caracteres |
| `email` | `Email` | sí | único, normalizado a minúsculas |
| `identityDocument` | `IdentityDocument` | sí | único |
| `role` | `UserRole` | sí | **inmutable** (RG-02) |
| `status` | `UserStatus` | sí | inicia `ACTIVE` |
| `passwordHash` | `PasswordHash` | sí | opaco; nunca se expone |
| `authTokenVersion` | `long` | sí | inicia `0`; se incrementa para invalidar JWT |

**Comportamiento**

| Método | Precondición | Efecto / excepción |
|---|---|---|
| `create(fullName, email, identityDocument, role, passwordHash)` | todos no nulos | crea `ACTIVE`, versión `0`, UUID |
| `reconstitute(...)` | datos persistidos válidos | reconstruye sin generar UUID |
| `block()` | `status == ACTIVE` | `status = BLOCKED`; `invalidateTokens()` |
| `activate()` | `status == BLOCKED o INACTIVE` | `status = ACTIVE`; `invalidateTokens()` |
| `deactivate()` | `status != INACTIVE` | `status = INACTIVE`; `invalidateTokens()` |
| `invalidateTokens()` | — | `authTokenVersion++` |
| `assertCanOperate()` | `status == ACTIVE` | si no, `InactiveUserException` |
| `hasRole(UserRole role)` | — | comparación por catálogo |
| `changeFullName(FullName)` | valor válido | actualiza |
| `changeEmail(Email)` | valor válido | actualiza (unicidad la valida `UserService`) |
| `changeIdentityDocument(IdentityDocument)` | valor válido | actualiza (unicidad en servicio) |
| `changePasswordHash(PasswordHash)` | valor válido | actualiza |

## 3. Dominio 2 — Gestión de Compradores

### `Buyer`

| Atributo | Tipo | Obligatorio | Notas |
|---|---|---|---|
| `id` | `String` | sí | UUID |
| `user` | `User` | sí | debe tener rol `BUYER` |
| `mainAddress` | `Address` | sí | dirección de entrega principal |
| `additionalAddresses` | `List<Address>` | no | vista inmutable |
| `commercialStatus` | `CommercialStatus` | sí | inicia `ELIGIBLE` |

**Comportamiento:** `create(user, mainAddress)`, `reconstitute(...)`,
`addAdditionalAddress(Address)`, `removeAdditionalAddress(Address)`,
`changeMainAddress(Address)`, `restrict()`, `suspend()`, `markEligible()`,
`assertCanPurchase()` (si no es `ELIGIBLE` ⇒ `RestrictedBuyerException`),
`isEligible()`. Un comprador nunca administra inventarios ni datos de otros
compradores (la autorización se resuelve en los servicios).

## 4. Dominio 3 — Gestión de Vendedores

### `Seller`

| Atributo | Tipo | Obligatorio | Notas |
|---|---|---|---|
| `id` | `String` | sí | UUID |
| `user` | `User` | sí | debe tener rol `SELLER` |
| `taxId` | `TaxId` | sí | único en la plataforma |
| `warehouses` | `List<Warehouse>` | no | vista inmutable; relación bidireccional (A-05) |

**Comportamiento**

| Método | Regla |
|---|---|
| `registerBy(adminActor, sellerUser, taxId)` | `adminActor` debe ser `ADMIN` y estar `ACTIVE`; si no ⇒ `SelfRegistrationNotAllowedException`. `sellerUser.role == SELLER`; si no ⇒ `InvalidRoleException`. |
| `assignWarehouse(Warehouse)` | la bodega debe ser `SELLER`, pertenecer a este vendedor y no estar ya asignada; si no ⇒ `InvalidWarehouseTypeException` / `WarehouseAlreadyAssignedException`. |
| `hasWarehouses()` | `!warehouses.isEmpty()` |
| `updateTaxId(TaxId)` | actualiza; la unicidad la valida `SellerService`. |

## 5. Dominio 4 — Gestión de Bodegas

### `Warehouse`

| Atributo | Tipo | Obligatorio | Notas |
|---|---|---|---|
| `id` | `String` | sí | UUID |
| `name` | `String` | sí | texto libre, no vacío |
| `location` | `Address` | sí | ubicación física |
| `type` | `WarehouseType` | sí | `MARKETPLACE` o `SELLER` |
| `owner` | `Seller` | según tipo | obligatorio si `SELLER`; **null** si `MARKETPLACE` |
| `responsible` | `User` | sí | rol `ADMIN` o `LOGISTIC_OPERATOR`, `ACTIVE` |

**Factorías:** `createMarketplace(name, location, responsible)` y
`createSellerWarehouse(name, location, owner, responsible)`.
**Comportamiento:** `changeName`, `changeLocation`, `assignResponsible(User)`,
`assertOperational()` (responsable presente). Tipos inválidos ⇒
`InvalidWarehouseTypeException`.

## 6. Dominio 5 — Gestión del Catálogo

### `Product`

| Atributo | Tipo | Obligatorio | Notas |
|---|---|---|---|
| `id` | `String` | sí | UUID |
| `seller` | `Seller` | sí | dueño del producto |
| `name` | `String` | sí | texto libre, no vacío |
| `description` | `String` | sí | texto libre |
| `type` | `ProductType` | sí | `PHYSICAL` / `DIGITAL` |
| `status` | `ProductStatus` | sí | inicia `SUSPENDED` (D-13) |
| `price` | `Money` | sí | > 0, IVA incluido |
| `variants` | `List<ProductVariant>` | no | vista inmutable |

**Comportamiento**

| Método | Precondición | Efecto / excepción |
|---|---|---|
| `create(seller, name, description, type, price)` | precio > 0 | nace `SUSPENDED` |
| `publish()` | no `DISCONTINUED`; si físico, `seller.hasWarehouses()` | `status = PUBLISHED`; `InvalidProductStateException` / `SellerWithoutWarehouseException` |
| `suspend()` | `PUBLISHED` | `status = SUSPENDED`; si `DISCONTINUED` ⇒ `InvalidProductStateException` |
| `discontinue()` | no `DISCONTINUED` | `status = DISCONTINUED` (terminal) |
| `changePrice(Money)` | `> 0` | actualiza; si no ⇒ `InvalidPriceException` |
| `changeDetails(name, description)` | no vacíos | actualiza; `DISCONTINUED` ⇒ `InvalidProductStateException` |
| `addVariant(ProductVariant)` | variante válida y no duplicada | añade |
| `removeVariant(ProductVariant)` | existe | elimina |
| `requiresInventory()` | — | `type == PHYSICAL` |
| `requiresShipment()` | — | `type == PHYSICAL` |
| `isPurchasable()` | — | `status == PUBLISHED` |

La comprobación de existencia de inventario al publicar la hace
`CatalogService` con `InventoryRepositoryPort.existsByProductId` (D-13).

## 7. Dominio 6 — Gestión del Inventario

### `Inventory`

| Atributo | Tipo | Obligatorio | Notas |
|---|---|---|---|
| `id` | `String` | sí | UUID |
| `product` | `Product` | sí | debe ser físico |
| `warehouse` | `Warehouse` | sí | bodega que almacena |
| `availableQuantity` | `int` | sí | ≥ 0 |
| `reservedQuantity` | `int` | sí | ≥ 0 |
| `condition` | `InventoryCondition` | sí | inicia `GOOD` |

**Comportamiento**

| Método | Regla |
|---|---|
| `create(product, warehouse, initialQuantity)` | producto físico (`InvalidProductTypeException`), cantidad ≥ 0; crea `GOOD` |
| `reserve(n)` | `condition == GOOD` (`DamagedInventoryException`), `n > 0` (`InvalidReservationException`), `n <= available` (`InsufficientStockException`); `available -= n`, `reserved += n` |
| `releaseReservation(n)` | `n > 0` y `n <= reserved` (`InvalidReservationException`); `reserved -= n`, `available += n` |
| `confirmSale(n)` | `n > 0` y `n <= reserved` (`InvalidReservationException`); `reserved -= n` |
| `adjust(delta)` | resultado `available + delta >= 0` (`NegativeStockException`); aplica |
| `registerReturn(n)` | `condition == GOOD` (`DamagedInventoryException`), `n > 0`; `available += n` |
| `markDamaged()` | `condition == GOOD` y `reserved == 0` (`InvalidReservationException`); `condition = DAMAGED` |
| `markRepaired()` | `condition == DAMAGED`; `condition = GOOD` |

Invariante global: `availableQuantity >= 0` y `reservedQuantity >= 0` **siempre**.

### `InventoryMovement` (inmutable)

| Atributo | Tipo | Notas |
|---|---|---|
| `id` | `String` | UUID |
| `inventory` | `Inventory` | inventario afectado |
| `type` | `MovementType` | `INFLOW`, `RESERVATION`, `SALE`, `ADJUSTMENT`, `RETURN` |
| `quantity` | `int` | positivo salvo `ADJUSTMENT`, que puede ser negativo; nunca 0 |
| `performedBy` | `User` | actor autenticado |
| `occurredAt` | `Instant` | tomado de `ClockPort` |

Factoría `create(...)`; sin setters.

## 8. Dominio 7 — Gestión del Ciclo de Pedidos

### `Cart` / `CartItem`

`Cart`: `id`, `buyer`, `items`. `CartItem`: `product`, `quantity`, `unitPrice`
(snapshot del precio al añadir). No tiene repositorio propio.

**Comportamiento de `Cart`:** `create(Buyer)`, `addItem(Product, int)`
(agrupa duplicados sumando cantidades; producto `PUBLISHED`; comprador
`ELIGIBLE`), `updateQuantity(productId, int)`, `removeItem(productId)`,
`clear()`, `total()`. `CartItem.changeQuantity(int)` exige cantidad > 0.

### `Order`

| Atributo | Tipo | Notas |
|---|---|---|
| `id` | `String` | UUID |
| `buyer` | `Buyer` | dueño del pedido |
| `items` | `List<CartItem>` | copia inmutable de la selección |
| `status` | `OrderStatus` | inicia `PENDING_PAYMENT` |
| `totalAmount` | `Money` | suma de subtotales |
| `paymentStatus` | `PaymentStatus` | inicia `PENDING` |
| `allocations` | `List<StockAllocation>` | reservas por producto/bodega |
| `deliveryAddress` | `Address` | destino |
| `invoice` | `Invoice` | nula hasta confirmar pago |
| `createdAt` | `Instant` | `ClockPort` |

**Máquina de estados**

```
fromCart → PENDING_PAYMENT
PENDING_PAYMENT --confirmPayment--> PAID
PAID --markDispatched--> DISPATCHED        (solo físico)
PAID --markDelivered--> DELIVERED          (solo digital, D-08)
DISPATCHED --markDelivered--> DELIVERED    (físico)
DELIVERED --finalizeOrder--> FINALIZED
PENDING_PAYMENT --cancel--> CANCELLED      (única cancelación)
```

Cualquier otra transición ⇒ `InvalidStateTransitionException`.

**Comportamiento:** `fromCart(cart, deliveryAddress, createdAt)` (carrito vacío
⇒ `EmptyCartException`), `reconstitute(...)`, `confirmPayment(Instant)` (fija
`paymentStatus = CONFIRMED`), `markDispatched()`, `markDelivered()`,
`finalizeOrder()`, `cancel()`, `attachInvoice(Invoice)`, `assertModifiable()`
(`DELIVERED`/`FINALIZED` ⇒ `FinalizedOrderException`; `CANCELLED` ⇒
`CancelledOrderException`), `requiresShipment()` (algún ítem físico),
`isDigitalOnly()` (todos digitales).

### `Invoice`

| Atributo | Tipo | Notas |
|---|---|---|
| `id` | `String` | UUID |
| `order` | `Order` | pedido pagado |
| `number` | `String` | `INV-<orderId>` (A-10) |
| `subtotal` | `Money` | `total / (1 + tasa)` |
| `taxes` | `Money` | `total − subtotal` |
| `total` | `Money` | igual a `order.totalAmount` |
| `status` | `InvoiceStatus` | `ISSUED` / `VOIDED` |
| `issuedAt` | `Instant` | `ClockPort` |

`create(order, number, taxRate, issuedAt)` exige `order.status == PAID`.
`voidInvoice()` una sola vez (`VoidedInvoiceException` si ya está anulada).
Una factura por pedido (restricción única `invoices.order_id`).

### `Shipment`

| Atributo | Tipo | Notas |
|---|---|---|
| `id` | `String` | UUID |
| `order` | `Order` | pedido físico |
| `originWarehouse` | `Warehouse` | bodega de la asignación |
| `destination` | `Address` | dirección de entrega |
| `trackingNumber` | `String` | obligatorio al iniciar |
| `status` | `ShipmentStatus` | `PENDING` → `IN_TRANSIT` → `DELIVERED` |

`create(order, originWarehouse, destination)` solo para pedidos físicos.
`startShipping(trackingNumber)` exige `PENDING` y tracking no vacío
(`ShipmentAlreadyStartedException` si no); `confirmDelivery()` exige
`IN_TRANSIT` (`InvalidStateTransitionException` si no).

### `Return`

| Atributo | Tipo | Notas |
|---|---|---|
| `id` | `String` | UUID |
| `order` | `Order` | pedido entregado/finalizado |
| `reason` | `String` | texto libre no vacío |
| `status` | `ReturnStatus` | `REQUESTED` → `APPROVED`/`REJECTED`; `APPROVED` → `COMPLETED` |
| `requestedAt` | `Instant` | `ClockPort` |

`request(order, reason, requestedAt)` exige `DELIVERED`/`FINALIZED`
(`ReturnNotAllowedException`). `approve()`, `reject()` y `complete()` validan
transiciones (`ReturnAlreadyProcessedException`).

### `Refund`

| Atributo | Tipo | Notas |
|---|---|---|
| `id` | `String` | UUID |
| `returnRequest` | `Return` | devolución `APPROVED`/`COMPLETED` |
| `amount` | `Money` | > 0 y ≤ total del pedido |
| `status` | `RefundStatus` | `PENDING` → `PROCESSED` |

`create(returnRequest, amount, orderTotal)` valida D-15;
`process()` una sola vez (`RefundAlreadyProcessedException`).

## 9. Auditoría

### `AuditLog` (inmutable)

| Atributo | Tipo | Notas |
|---|---|---|
| `id` | `String` | UUID |
| `action` | `AuditAction` | catálogo de acciones auditables |
| `entityType` | `AuditEntityType` | tipo de entidad afectada (A-02) |
| `entityId` | `String` | id de la entidad |
| `performedBy` | `String` | id del usuario actor |
| `role` | `UserRole` | rol del actor en el momento |
| `occurredAt` | `Instant` | `ClockPort` |
| `details` | `String` | texto libre descriptivo |

## 10. Modelos de resultado

| Modelo | Atributos |
|---|---|
| `AuthResult` | `token: String`, `user: User` |
| `SalesReport` | `from: LocalDate`, `to: LocalDate`, `orderCount: int`, `totalSales: Money` |
| `OrderSummary` | `totalOrders`, `pendingPayment`, `paid`, `dispatched`, `delivered`, `finalized`, `cancelled` (todos `int`) |
| `InventorySummary` | `totalRecords`, `totalAvailable`, `totalReserved`, `damagedRecords` (`int`) |
| `ReturnSummary` | `totalReturns`, `requested`, `approved`, `rejected`, `completed` (`int`) |
| `UserSummary` | `totalUsers: int`, `usersByRole: Map<UserRole, Integer>` |

## 11. Invariantes transversales

1. Toda operación de servicio exige `User` actor `ACTIVE` (RG-01) y rol
   autorizado (RG-03).
2. `User.role` nunca cambia después de la creación (RG-02).
3. `User.email` y `User.identityDocument` son únicos (validado en servicio y por
   restricción de base de datos).
4. `Seller` solo se crea con actor `ADMIN` (R-08).
5. Ningún `Inventory` puede quedar con cantidades negativas (R-06).
6. `InventoryCondition.DAMAGED` no permite reserva ni venta (R-06).
7. `Order` en `DELIVERED`, `FINALIZED` o `CANCELLED` no admite modificación de
   datos; solo transiciones válidas (R-07).
8. Los productos digitales no generan inventario ni envíos (D-07/D-08).
9. Todo cambio de estado relevante genera `AuditLog` (D-14).
