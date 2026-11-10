# `ReturnService`

**OBJ:** OBJ-11 · **Acceso:** `BUYER` (solicita), `ADMIN` (aprueba/rechaza), `LOGISTIC_OPERATOR` (recibe)
**Paquete:** `application.domain.services.ReturnService`

## Dependencias (constructor)

`ReturnRepositoryPort`, `OrderRepositoryPort`, `InventoryService`, `AuditPort`,
`ClockPort`.

## Métodos

### `Return requestReturn(User actor, String orderId, String reason)`

| Elemento | Detalle |
|---|---|
| Precondiciones | `BUYER` dueño del pedido; pedido `DELIVERED` o `FINALIZED`; `reason` no vacío |
| Flujo | Carga pedido (`OrderNotFoundException`); `Return.request(order, reason, clock.now())` (`ReturnNotAllowedException` si el estado no lo permite); guarda. |
| Auditoría | `RETURN_REQUESTED` sobre `RETURN` |

### `Return requestReturn(User actor, String orderId, String reason, String productId, int quantity)`

| Elemento | Detalle |
|---|---|
| Precondiciones | Las anteriores más `productId` perteneciente al pedido y `0 < quantity <=` cantidad comprada |
| Flujo | Crea una devolución parcial con producto/cantidad; los campos quedan nulos en la solicitud legacy de pedido completo. |
| Auditoría | `RETURN_REQUESTED` sobre `RETURN` |

### `Return approve(User actor, String returnId)` / `reject`

- Solo `ADMIN`. Carga devolución (`ReturnNotFoundException`); `approve()` o
  `reject()` (si ya fue procesada ⇒ `ReturnAlreadyProcessedException`); guarda.
- **Auditoría:** `RETURN_APPROVED` / `RETURN_REJECTED` sobre `RETURN`.

### `Return complete(User actor, String returnId)`

- Solo `LOGISTIC_OPERATOR`. `return.complete()` (`APPROVED → COMPLETED`). Si la
  devolución es parcial, reingresa **solo** `productId` por `quantity`; si es
  legacy, reingresa cada ítem físico del pedido. Busca la asignación del producto
  y llama `InventoryService.registerReturn(actor, productId, warehouseId, qty)`
  (movimiento `RETURN`). Guarda la devolución. El reingreso y el cambio de
  estado comparten la transacción del caso de uso; un fallo revierte ambos.
- **Auditoría:** `RETURN_COMPLETED` sobre `RETURN`.

### `List<Return> listReturns(User actor)`

- `BUYER` propias; `ADMIN`, `LOGISTIC_OPERATOR`, `SUPERVISOR` todas.

### `Return getReturn(User actor, String returnId)`

- Mismas reglas de visibilidad que `listReturns`.

## Reglas

- Solo se solicita devolución de pedidos entregados/finalizados (A-12).
- La devolución parcial exige un producto del pedido y cantidad entre 1 y la
  cantidad comprada; el reembolso posterior lo calcula el Admin con el monto
  recibido en `RefundService`.
- Una devolución `REJECTED` o `COMPLETED` no vuelve a procesarse.
- Los productos digitales no reingresan stock.
- El reingreso exige inventario en condición `GOOD`.
