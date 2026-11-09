# `OrderLifecycleService`

**OBJ:** OBJ-08 — **Acceso:** `BUYER` (propio), `ADMIN`
**Paquete:** `application.domain.services.OrderLifecycleService`

## Trazabilidad

| REQ | Sección del PDF | Regla cubierta |
|---|---|---|
| REQ-027 | p.4 §6.1.8 | El pedido se finaliza tras la entrega confirmada. |
| REQ-052 | p.8 §11 | Un pedido finalizado no puede modificarse. |
| REQ-068 | p.7 Dominio 7 | Cancelación: supuesto no exigido por el PDF. |
| REQ-070 | p.7 Dominio 7 | Separación `DELIVERED`/`FINALIZED` derivada. |

## Responsabilidad

Gestiona cancelación y finalización del pedido, incluida la liberación de
reservas al cancelar. Es invocado por `OrderService.cancel` y
`OrderService.finalize`.

## Reglas

- Cancelar: solo el `BUYER` dueño y solo desde `PENDING_PAYMENT`; libera cada
  asignación con `Inventory.releaseReservation` y registra `RESERVATION`.
- Finalizar: `BUYER` dueño o `ADMIN`; transición `DELIVERED → FINALIZED`.
- Auditoría: `ORDER_STATUS_CHANGED` sobre `ORDER`.

## Pruebas

`OrderLifecycleServiceTest`: cancelación con liberación y rechazo de no dueño;
finalización de pedido entregado.
