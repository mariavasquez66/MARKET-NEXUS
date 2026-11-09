# `InventoryQueryService`

**OBJ:** OBJ-06 — **Acceso:** `SELLER` (propio), `LOGISTIC_OPERATOR`, `SUPERVISOR`, `ADMIN`
**Paquete:** `application.domain.services.InventoryQueryService`

## Trazabilidad

| REQ | Sección del PDF | Regla cubierta |
|---|---|---|
| REQ-007 | p.1 §2 | Administrar el inventario distribuido. |
| REQ-045 | p.7 Dominio 6 | Inventario vinculado a producto y bodega. |
| REQ-059 | p.8 §12 | Administración de inventario por vendedor y operador logístico. |

## Responsabilidad

Resuelve consultas de inventario por producto, bodega, vendedor o del propio
vendedor autenticado. Es invocado por `InventoryService`.

## Reglas

- `SELLER` solo consulta inventario de productos propios; cualquier inventario
  ajeno en la colección produce `UnauthorizedOperationException`.
- `getAvailable` permite `SELLER`, `LOGISTIC_OPERATOR`, `ADMIN` y `SUPERVISOR`.
- `listBySeller` y `listByProduct` permiten `SELLER`, `ADMIN` y `SUPERVISOR`.
- `listMyInventory` exige `SELLER`.
- Inventario inexistente produce `InventoryNotFoundException`.

## Pruebas

`InventoryQueryServiceTest`: lectura propia y rechazo de vendedor ajeno; listado
propio filtrado sobre todos los inventarios.
