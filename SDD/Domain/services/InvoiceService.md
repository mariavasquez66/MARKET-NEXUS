# `InvoiceService`

**OBJ:** OBJ-09 · **Acceso:** `BUYER` (consulta propia), `ADMIN` (anulación), `SUPERVISOR` (lectura)
**Paquete:** `application.domain.services.InvoiceService`

## Dependencias (constructor)

`InvoiceRepositoryPort`, `OrderRepositoryPort`, `TaxPolicyPort`, `AuditPort`,
`ClockPort`.

## Métodos

### `Invoice generateInvoice(User actor, String orderId)`

| Elemento | Detalle |
|---|---|
| Precondiciones | pedido existe (`OrderNotFoundException`) y está `PAID` (`InvalidStateTransitionException`); no existe factura previa (`InvoiceAlreadyExistsException`) |
| Flujo | 1) `taxRate = taxPolicy.currentTaxRate()` (por defecto `0.19`). 2) `number = "INV-" + orderId` (A-10). 3) `Invoice.create(order, number, taxRate, clock.now())`: `total = order.totalAmount`, `subtotal = total/(1+rate)`, `taxes = total − subtotal`. 4) Guarda y devuelve. |
| Excepciones | `OrderNotFoundException`, `InvalidStateTransitionException`, `InvoiceAlreadyExistsException` |
| Auditoría | `INVOICE_ISSUED` sobre `INVOICE` |

### `Invoice getInvoice(User actor, String invoiceId)`

- `BUYER` dueño del pedido, `ADMIN` o `SUPERVISOR`. No existe ⇒
  `InvoiceNotFoundException`.

### `Invoice getByOrder(User actor, String orderId)`

- Mismas reglas; si no hay factura ⇒ `InvoiceNotFoundException`.

### `List<Invoice> listByBuyer(User actor, String buyerId)`

- `BUYER` propio, `ADMIN` o `SUPERVISOR`.

### `Invoice voidInvoice(User actor, String invoiceId)`

- Solo `ADMIN`. `invoice.voidInvoice()`; si ya estaba `VOIDED` ⇒
  `VoidedInvoiceException`. Guarda.
- **Auditoría:** `INVOICE_VOIDED` sobre `INVOICE`.

## Reglas

- Una factura por pedido pagado (única por `order_id`).
- Los importes usan `Money` (escala 2, `HALF_UP`).
- Una factura anulada no se puede volver a anular.
- La factura no modifica el estado del pedido.
