# `BuyerService`

**OBJ:** OBJ-03 · **Acceso:** `BUYER` (lo propio) y `ADMIN` (gestión de estado)
**Paquete:** `application.domain.services.BuyerService`

## Dependencias (constructor)

`BuyerRepositoryPort`, `AuditPort`, `ClockPort`.

## Métodos

### `Buyer getBuyer(User actor, String buyerId)`

- `BUYER` solo si `buyer.user.id == actor.id`; `ADMIN` cualquiera. Si no ⇒
  `UnauthorizedOperationException`. No existe ⇒ `BuyerNotFoundException`.

### `Buyer getBuyerByUser(User actor)`

- `BUYER` únicamente; obtiene el comprador asociado a `actor.id`
  (`BuyerNotFoundException` si no existe).

### `List<Address> listAddresses(User actor)`

- `BUYER` propio; devuelve dirección principal + adicionales.

### `Buyer addAdditionalAddress(User actor, Address address)`

- `BUYER` propio; `buyer.addAdditionalAddress(address)` y guarda.
- Dirección inválida ⇒ `InvalidAddressException`.

### `Buyer changeMainAddress(User actor, Address address)`

- `BUYER` propio; `buyer.changeMainAddress(address)` y guarda.

### `Buyer restrict(User actor, String buyerId)` / `suspend` / `markEligible`

- Solo `ADMIN`. Carga el comprador (`BuyerNotFoundException`) y aplica
  `restrict()`, `suspend()` o `markEligible()`.
- Un comprador `RESTRICTED`/`SUSPENDED` no puede comprar
  (`RestrictedBuyerException` en `assertCanPurchase`).

## Reglas

- El comprador nunca administra inventarios ni datos de otros compradores.
- Las operaciones de compra delegan siempre en `buyer.assertCanPurchase()`.
- No se permite cambiar el `User` asociado ni el rol.
