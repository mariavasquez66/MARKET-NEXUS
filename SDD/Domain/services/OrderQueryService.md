# `OrderQueryService`

**OBJ:** OBJ-08 — **Acceso:** `BUYER`, `SELLER`, `LOGISTIC_OPERATOR`, `SUPERVISOR`, `ADMIN`
**Paquete:** `application.domain.services.OrderQueryService`

## Trazabilidad

| REQ | Sección del PDF | Regla cubierta |
|---|---|---|
| REQ-009 | p.1 §2 | Control del ciclo completo de los pedidos. |
| REQ-036 | p.6 Dominio 2 | Un comprador no accede a datos de otros compradores. |
| REQ-060 | p.8 §12 | Gestión de pedidos por comprador, vendedor y operador logístico. |
| REQ-051 | p.7 Dominio 7 | Consulta por estado del pedido. |

## Responsabilidad

Resuelve consultas y listados de pedidos con autorización por rol y propiedad.
Es invocado por `OrderService.getOrder`, `listOrders` y `listByStatus`.

## Reglas

- `BUYER` solo ve pedidos propios.
- `SELLER` solo ve pedidos que contienen algún producto suyo.
- `LOGISTIC_OPERATOR`, `SUPERVISOR` y `ADMIN` ven cualquiera.
- Filtrar por estado requiere un estado no nulo.
- Auditoría: no muta estado; no genera eventos.

## Pruebas

`OrderQueryServiceTest`: lectura de pedido propio y rechazo de otro comprador,
listado propio y filtrado por productos del vendedor.
