# `CartService`

**OBJ:** OBJ-07 · **Acceso:** `BUYER`
**Paquete:** `application.domain.services.CartService`

## Dependencias (constructor)

`CartRepositoryPort`, `BuyerRepositoryPort`, `ProductRepositoryPort`, `AuditPort`,
`ClockPort`.

## Métodos

### `Cart getCart(User actor)`

- `BUYER`; busca el carrito por `buyer.id`. Si no existe, crea uno vacío
  (`Cart.create(buyer)`) y lo guarda. Devuelve el carrito con ítems inmutables.

### `Cart addItem(User actor, String productId, int quantity)`

- `BUYER`; `quantity > 0` (`InvalidQuantityException`). Carga el producto
  (`ProductNotFoundException`) y exige `isPurchasable()`
  (`ProductNotAvailableException`). `buyer.assertCanPurchase()`.
  `cart.addItem(product, quantity)` agrupa duplicados sumando cantidades y toma
  el precio actual como `unitPrice`. Guarda. **Auditoría:** `CART_ITEM_ADDED`
  sobre `CART`, con `productId xQuantity`.

### `Cart updateQuantity(User actor, String productId, int quantity)`

- `BUYER`; `quantity > 0`; `cart.updateQuantity(...)`. Si el producto no está en
  el carrito ⇒ `InvalidDataException`. Guarda. **Auditoría:**
  `CART_ITEM_UPDATED` sobre `CART`, con `productId xQuantity`.

### `Cart removeItem(User actor, String productId)`

- `BUYER`; `cart.removeItem(...)`; si no existe ⇒ `InvalidDataException`. Guarda.
  **Auditoría:** `CART_ITEM_REMOVED` sobre `CART`, con `productId`.

### `void clearCart(User actor)`

- `BUYER`; `cart.clear()` y guarda. **Auditoría:** `CART_CLEARED` sobre `CART`.

## Reglas

- Un carrito pertenece a un único comprador.
- Solo se añaden productos `PUBLISHED`.
- Un comprador `RESTRICTED`/`SUSPENDED` no puede modificar el carrito ni hacer
  checkout (`RestrictedBuyerException`).
- El precio del `CartItem` es una foto; el total definitivo se recalcula en
  `Order.fromCart`.
