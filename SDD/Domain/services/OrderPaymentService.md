# `OrderPaymentService`

**OBJ:** OBJ-08 — **Acceso:** `BUYER` (dueño del pedido)
**Paquete:** `application.domain.services.OrderPaymentService`

## Trazabilidad

| REQ | Sección del PDF | Regla cubierta |
|---|---|---|
| REQ-025 | p.4 §6.1.6 | Se valida el pago y se inicia la preparación. |
| REQ-042 | p.6-7 Dominio 5 | El producto digital se entrega tras el pago. |
| REQ-046 | p.7 Dominio 6 | Movimiento de salida por venta. |
| REQ-051 | p.7 Dominio 7 | Transición `PENDING_PAYMENT → PAID`. |

## Responsabilidad

Confirma el pago del pedido, consume la reserva de inventario, genera la factura
y marca entrega inmediata para pedidos solo digitales. Es invocado por
`OrderService.confirmPayment`.

## Reglas

- Actor `BUYER` dueño; cualquier otro comprador recibe
  `UnauthorizedOperationException`.
- Cada asignación ejecuta `Inventory.confirmSale` y un movimiento `SALE`.
- Se genera y adjunta la factura mediante `InvoiceService`.
- El pago registra `PaymentStatus.CONFIRMED` y `paidAt` con la hora del `ClockPort`.
- Si el pedido es solo digital, pasa a `DELIVERED` sin envío.
- Auditoría: `ORDER_STATUS_CHANGED` sobre `ORDER`.

## Pruebas

`OrderPaymentServiceTest`: pago con factura, `paidAt` y movimiento; entrega
digital inmediata y rechazo de comprador no dueño.
