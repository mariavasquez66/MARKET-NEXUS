# `BuyerEligibilityService`

**OBJ:** OBJ-03 — **Acceso:** `ADMIN`
**Paquete:** `application.domain.services.BuyerEligibilityService`

## Trazabilidad

| REQ | Sección del PDF | Regla cubierta |
|---|---|---|
| REQ-004 | p.1 §2 | Administrar compradores registrados. |
| REQ-035 | p.6 Dominio 2 | Estado comercial obligatorio y condición para comprar. |
| REQ-071 | p.6 Dominio 2 | Catálogo `ELIGIBLE/RESTRICTED/SUSPENDED`: supuesto. |

## Responsabilidad

Cambia el estado comercial del comprador (`RESTRICTED`, `SUSPENDED`,
`ELIGIBLE`). Es invocado por `BuyerService`.

## Reglas

- Solo `ADMIN`; otro rol produce `InsufficientPermissionException`.
- El comprador debe existir (`BuyerNotFoundException`).
- Cada cambio persiste el comprador mediante `BuyerRepositoryPort`.
- `Buyer.assertCanPurchase` bloquea compras cuando no está `ELIGIBLE`.

## Pruebas

`BuyerEligibilityServiceTest`: restricción y reactivación por administrador, y
rechazo de rol no administrador.
