# `OrderFulfillmentService`

**OBJ:** OBJ-08/10 — **Acceso:** `LOGISTIC_OPERATOR`, `ADMIN`
**Paquete:** `application.domain.services.OrderFulfillmentService`

## Trazabilidad

| REQ | Sección del PDF | Regla cubierta |
|---|---|---|
| REQ-026 | p.4 §6.1.7 | Empaque, despacho y transporte del pedido. |
| REQ-051 | p.7 Dominio 7 | Transiciones `PAID → DISPATCHED → DELIVERED`. |
| REQ-063 | p.2 §3.1 | Los envíos están incluidos en el alcance. |
| REQ-069 | p.2 §3.1 | Agrupación de envíos: interpretación derivada. |

## Responsabilidad

Autoriza el paso del pedido a despachado o entregado solo cuando todos los
envíos de cada asignación alcanzaron el estado requerido. Es invocado por
`OrderService.markDispatched` y `OrderService.markDelivered`.

## Reglas

- Solo `LOGISTIC_OPERATOR` o `ADMIN`.
- Un pedido sin asignaciones no puede despacharse ni entregarse.
- Para despachar, cada asignación debe tener al menos un envío y todos deben
  estar `IN_TRANSIT` o `DELIVERED`.
- Para entregar, todos los envíos de cada asignación deben estar `DELIVERED`.
- Auditoría: `ORDER_STATUS_CHANGED` sobre `ORDER`.

## Pruebas

`OrderFulfillmentServiceTest`: despacho exige todos los envíos en tránsito y
entrega exige todos los envíos entregados.
