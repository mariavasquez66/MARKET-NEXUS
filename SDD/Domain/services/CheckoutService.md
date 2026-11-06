# `CheckoutService`

**OBJ:** OBJ-08 — **Acceso:** `BUYER` (propio)
**Paquete:** `application.domain.services.CheckoutService`

## Trazabilidad

| REQ | Sección del PDF | Regla cubierta |
|---|---|---|
| REQ-019 | p.4 §6.1 | El checkout forma parte del flujo compra → transacción. |
| REQ-024 | p.4 §6.1.5 | El comprador confirma el pedido desde el carrito. |
| REQ-041 | p.6-7 Dominio 5 | Los productos físicos requieren inventario y despacho. |
| REQ-045 | p.7 Dominio 6 | El inventario se vincula a producto y bodega. |
| REQ-047/048/049 | p.7 Dominio 6; p.8 §11 | Sin stock negativo, sin reservar inexistente ni dañado. |
| REQ-051 | p.7 Dominio 7 | El pedido nace en `PENDING_PAYMENT`. |

## Responsabilidad

Crea el pedido desde el carrito del comprador, reserva stock entre bodegas y
compensa las reservas si falla la persistencia del pedido o el stock es
insuficiente. Es invocado por `OrderService.checkout`.

## Reglas

- Actor `BUYER`; el comprador debe estar `ELIGIBLE` y el carrito no vacío.
- Solo productos publicados (`isPurchasable`).
- Los productos físicos reservan en varias bodegas, de mayor a menor
  disponibilidad; si no alcanza, se liberan las reservas ya hechas.
- Cualquier fallo posterior a una reserva libera las reservas antes de propagar.
- El carrito se vacía y se guarda dentro de la transacción del caso de uso.
- Auditoría: `ORDER_CREATED` sobre `ORDER`.

## Pruebas

`CheckoutServiceTest`: reserva y vaciado, compensación ante fallo de guardado y
rechazo por stock insuficiente.
