# `InventoryService`

**OBJ:** OBJ-06 · **Acceso:** `SELLER` (alta/consulta propia), `LOGISTIC_OPERATOR` (movimientos), `BUYER` (reserva/liberación vía checkout)
**Paquete:** `application.domain.services.InventoryService`

Las lecturas y su autorización se delegan en `InventoryQueryService`; las
mutaciones de stock siguen en esta fachada.

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

### Operaciones internas de stock: `reserve`, `releaseReservation`, `confirmSale`

- Visibilidad de paquete: no forman parte de la API p��blica del servicio. Solo
  se usan desde pruebas y desde el flujo interno de pedidos; la mutaci��n
  p��blica de stock ocurre en `OrderService` (`checkout`, `cancel`,
  `confirmPayment`) dentro de la transacci��n del caso de uso.
- `reserve` carga inventario (`InventoryNotFoundException`), aplica
  `inventory.reserve(quantity)`, guarda y registra `RESERVATION`.
- `releaseReservation` aplica `inventory.releaseReservation(quantity)`, guarda y
  registra `RESERVATION`.
- `confirmSale` aplica `inventory.confirmSale(quantity)`, guarda y registra
  `SALE`.
- Errores: `InsufficientStockException`, `DamagedInventoryException`,
  `InvalidReservationException`.

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
- Si el actor es `SELLER`, **todos** los inventarios devueltos deben pertenecerle;
  se valida la colecci��n completa, no solo el primer elemento. Una lista vac��a
  es v��lida y no concede acceso a inventario ajeno.
- Violaci��n ��' `UnauthorizedOperationException`.

## Reglas

- `availableQuantity` y `reservedQuantity` nunca son negativas.
- `DAMAGED` no permite reservar, vender ni devolver stock.
- Todo movimiento genera un `InventoryMovement` con actor y `occurredAt`.
- Los productos digitales no tienen inventario.
- La transacci��n se delimita en el caso de uso (`@Transactional`).
