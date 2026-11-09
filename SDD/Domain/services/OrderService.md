# `OrderService`

**OBJ:** OBJ-08 · **Acceso:** `BUYER` (propio), `SELLER` (consulta), `LOGISTIC_OPERATOR`, `SUPERVISOR`/`ADMIN`
**Paquete:** `application.domain.services.OrderService`

`OrderService` es una fachada compatible que delega en `CheckoutService`,
`OrderPaymentService`, `OrderFulfillmentService`, `OrderLifecycleService` y
`OrderQueryService`. Las reglas siguientes describen el comportamiento observable
de la fachada.

## Dependencias (constructor)

`CartRepositoryPort`, `BuyerRepositoryPort`, `OrderRepositoryPort`,
`ProductRepositoryPort`, `InventoryRepositoryPort`,
`InventoryMovementRepositoryPort`, `ShipmentRepositoryPort`, `InvoiceService`,
`AuditPort`, `ClockPort`. La fachada construye con ellos los cinco servicios
delegados.

## Métodos

### `Order checkout(User actor, Address deliveryAddress)`

| Elemento | Detalle |
|---|---|
| Precondiciones | `BUYER`; carrito existente y no vacío (`CartNotFoundException`, `EmptyCartException`); comprador `ELIGIBLE` (`RestrictedBuyerException`); todos los productos siguen `PUBLISHED` (`ProductNotAvailableException`) |
| Flujo | 1) Carga carrito y comprador. 2) Agrupa ítems físicos. 3) Para cada producto físico, obtiene inventarios con `availableQuantity > 0` ordenados de mayor a menor y reserva en varias bodegas con `Inventory.reserve`, acumulando `StockAllocation`. Si no alcanza ⇒ libera las reservas ya hechas (`releaseReservation`) y lanza `InsufficientStockException`. 4) `Order.fromCart(cart, deliveryAddress, clock.now())` con las asignaciones. 5) Guarda pedido. 6) Vacía y guarda el carrito. Cualquier fallo posterior a una reserva ejecuta la misma compensación antes de propagar la excepción. |
| Excepciones | `CartNotFoundException`, `EmptyCartException`, `RestrictedBuyerException`, `ProductNotAvailableException`, `InsufficientStockException`, `DamagedInventoryException`, `InvalidQuantityException` |
| Auditoría | `ORDER_CREATED` sobre `ORDER` |

### `Order confirmPayment(User actor, String orderId)`

- `BUYER` dueño (`OrderNotFoundException`, `UnauthorizedOperationException`).
  `order.confirmPayment(clock.now())` (`PENDING_PAYMENT → PAID`,
  `InvalidStateTransitionException`). Para cada asignación ejecuta
  `Inventory.confirmSale` y guarda movimiento `SALE`. Genera la factura con
  `InvoiceService.generateInvoice(actor, orderId)` y la adjunta. Si el pedido es
  solo digital, ejecuta `order.markDelivered()` inmediatamente (D-08). Guarda.
- **Auditoría:** `ORDER_STATUS_CHANGED` sobre `ORDER` (incluye el detalle de la
  transición; si hay paso digital, se registra `PAID → DELIVERED`).

### `Order cancel(User actor, String orderId)`

- `BUYER` dueño. `order.cancel()` solo desde `PENDING_PAYMENT`
  (`InvalidStateTransitionException`). Libera cada asignación con
  `Inventory.releaseReservation`. Guarda.
- **Auditoría:** `ORDER_STATUS_CHANGED`.

### `Order markDispatched(User actor, String orderId)` / `markDelivered`

- `LOGISTIC_OPERATOR` (invocado por `LogisticsService`). Para `markDispatched`,
  cada asignación debe tener al menos un envío y **todos** sus envíos deben
  estar `IN_TRANSIT` o `DELIVERED`; para `markDelivered`, cada asignación debe
  tener al menos un envío y **todos** deben estar `DELIVERED`. Una lista vacía
  no autoriza la transición. Si no se cumple ⇒ `InvalidStateTransitionException`.
  Guarda.
- **Auditoría:** `ORDER_STATUS_CHANGED`.

### `Order finalize(User actor, String orderId)`

- `BUYER` dueño o `ADMIN`. `order.finalizeOrder()` (`DELIVERED → FINALIZED`);
  guarda y audita.

### `Order getOrder(User actor, String orderId)`

- `BUYER` dueño; `SELLER` si el pedido contiene algún producto suyo;
  `LOGISTIC_OPERATOR`, `SUPERVISOR`, `ADMIN` cualquiera. Si no ⇒
  `UnauthorizedOperationException`.

### `List<Order> listOrders(User actor)` / `listByStatus(User actor, OrderStatus status)`

- `BUYER` propios; `SELLER` pedidos con sus productos; `LOGISTIC_OPERATOR`,
  `SUPERVISOR`, `ADMIN` todos (con filtro opcional por estado).

## Reglas

- La reserva se reparte por mayor disponibilidad entre bodegas (D-08).
- Cancelación únicamente desde `PENDING_PAYMENT`.
- Un pedido `DELIVERED`/`FINALIZED`/`CANCELLED` no admite modificación de datos
  (R-07); solo transiciones válidas.
- Los productos digitales no generan asignaciones ni envíos.
- La transacción se delimita en el caso de uso; si falla cualquier escritura
  posterior a una reserva, `checkout` libera las reservas antes de propagar.
