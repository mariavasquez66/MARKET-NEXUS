# `InventoryService`

**OBJ:** OBJ-06 · **Acceso:** `SELLER` (alta/consulta propia), `LOGISTIC_OPERATOR` (movimientos), `BUYER` (reserva/liberación vía checkout)
**Paquete:** `application.domain.services.InventoryService`

## Dependencias (constructor)

`InventoryRepositoryPort`, `InventoryMovementRepositoryPort`,
`ProductRepositoryPort`, `WarehouseRepositoryPort`, `AuditPort`, `ClockPort`.

## Métodos

### `Inventory registerInitialStock(User actor, String productId, String warehouseId, int quantity)`

| Elemento | Detalle |
|---|---|
| Precondiciones | actor `SELLER` dueño del producto; producto físico (`InvalidProductTypeException`); bodega `SELLER` del mismo vendedor (`InvalidWarehouseTypeException` / `UnauthorizedOperationException`); `quantity >= 0` |
| Flujo | 1) Carga producto y bodega. 2) Verifica que no exista inventario para (producto, bodega) ⇒ `DuplicateEntityException`. 3) `Inventory.create(...)`. 4) Guarda. 5) Genera `InventoryMovement` `INFLOW` si `quantity > 0`. |
| Auditoría | `INVENTORY_MOVEMENT` sobre `INVENTORY` cuando hay movimiento |

### `Inventory addStock(User actor, String productId, String warehouseId, int quantity)`

- `LOGISTIC_OPERATOR` o `SELLER` dueño. `quantity > 0`
  (`InvalidQuantityException`). Aplica `inventory.adjust(+quantity)`, guarda y
  registra movimiento `INFLOW`.

### `StockAllocation reserve(User actor, String productId, String warehouseId, int quantity)`

- `BUYER` durante checkout (o `ADMIN` por soporte). Carga inventario
  (`InventoryNotFoundException`), aplica `inventory.reserve(quantity)` y guarda.
  Registra movimiento `RESERVATION`. Devuelve
  `StockAllocation(productId, warehouseId, quantity)`.
- Errores: `InsufficientStockException`, `DamagedInventoryException`,
  `InvalidReservationException`.

### `void releaseReservation(User actor, String productId, String warehouseId, int quantity)`

- `BUYER` (cancelación) o `ADMIN`. `inventory.releaseReservation(quantity)`,
  guarda y registra `RESERVATION` (liberación).

### `void confirmSale(User actor, String productId, String warehouseId, int quantity)`

- `BUYER` (pago) o `ADMIN`. `inventory.confirmSale(quantity)`, guarda y registra
  `SALE`.

### `Inventory adjust(User actor, String productId, String warehouseId, int delta)`

- Solo `LOGISTIC_OPERATOR`. `delta != 0`; `inventory.adjust(delta)` (nunca deja
  negativo). Guarda y registra `ADJUSTMENT` con `quantity = delta`.

### `void registerReturn(User actor, String productId, String warehouseId, int quantity)`

- Solo `LOGISTIC_OPERATOR` (recepción de devolución). `inventory.registerReturn`;
  guarda y registra `RETURN`.

### `Inventory markDamaged(User actor, String productId, String warehouseId)` / `markRepaired`

- Solo `LOGISTIC_OPERATOR`. Aplica `markDamaged()` / `markRepaired()` y guarda.
  Sin movimiento de inventario (cambio de condición).

### `Inventory getAvailable(User actor, String productId, String warehouseId)`

- `SELLER` dueño, `LOGISTIC_OPERATOR`, `ADMIN` o `SUPERVISOR`.

### `List<Inventory> listBySeller(User actor, String sellerId)` / `listByProduct`

- `SELLER` propio o `ADMIN`/`SUPERVISOR` (lectura).

## Reglas

- `availableQuantity` y `reservedQuantity` nunca son negativas.
- `DAMAGED` no permite reservar, vender ni devolver stock.
- Todo movimiento genera un `InventoryMovement` con actor y `occurredAt`.
- Los productos digitales no tienen inventario.
