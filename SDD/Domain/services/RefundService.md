# `RefundService`

**OBJ:** OBJ-11 · **Acceso:** `ADMIN` (genera/procesa), `BUYER`/`SUPERVISOR` (consulta)
**Paquete:** `application.domain.services.RefundService`

## Dependencias (constructor)

`RefundRepositoryPort`, `ReturnRepositoryPort`, `OrderRepositoryPort`,
`AuditPort`, `ClockPort`.

## Métodos

### `Refund generateRefund(User actor, String returnId, Money amount)`

| Elemento | Detalle |
|---|---|
| Precondiciones | actor `ADMIN`; devolución existe (`ReturnNotFoundException`); devolución `APPROVED` o `COMPLETED` (`ReturnNotAllowedException`); `amount > 0` y `amount <= order.totalAmount` (`InvalidRefundAmountException`) |
| Flujo | 1) Carga devolución y pedido. 2) Suma los reembolsos existentes de la devolución (`refundRepository.findByReturnId`). 3) Si `acumulado + amount > order.totalAmount` ⇒ `InvalidRefundAmountException` (D-15). 4) `Refund.create(return, amount, order.totalAmount)`. 5) Guarda. |
| Auditoría | `REFUND_CREATED` sobre `REFUND` |

### `Refund process(User actor, String refundId)`

- Solo `ADMIN`. Carga reembolso (`RefundNotFoundException`);
  `refund.process()` (`PENDING → PROCESSED`; si ya procesado ⇒
  `RefundAlreadyProcessedException`); guarda.
- **Auditoría:** `REFUND_PROCESSED` sobre `REFUND`.

### `List<Refund> listByReturn(User actor, String returnId)`

- `BUYER` dueño de la devolución, `ADMIN` o `SUPERVISOR`.

### `Refund getRefund(User actor, String refundId)`

- Mismas reglas de visibilidad.

## Reglas

- No se reembolsa sobre devoluciones no aprobadas.
- La suma de reembolsos de una devolución no supera el total del pedido.
- `process()` se ejecuta una sola vez.
- Procesar un reembolso no cambia por sí solo `Order.paymentStatus` (A-07).
