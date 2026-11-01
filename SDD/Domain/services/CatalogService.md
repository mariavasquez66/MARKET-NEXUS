# `CatalogService`

**OBJ:** OBJ-05 · **Acceso:** `SELLER` (gestión propia), público (consulta)
**Paquete:** `application.domain.services.CatalogService`

## Dependencias (constructor)

`ProductRepositoryPort`, `SellerRepositoryPort`, `InventoryRepositoryPort`,
`AuditPort`, `ClockPort`.

## Métodos

### `Product registerProduct(User actor, String name, String description, ProductType type, Money price)`

| Elemento | Detalle |
|---|---|
| Precondiciones | actor `SELLER` y `ACTIVE`; nombre no vacío; `price > 0`; si `type == PHYSICAL`, el vendedor debe tener ≥1 bodega (`SellerWithoutWarehouseException`) |
| Flujo | 1) Carga el `Seller` por `actor.id` (`SellerNotFoundException`). 2) `Product.create(seller, name, description, type, price)` (nace `SUSPENDED`). 3) Guarda. |
| Auditoría | `PRODUCT_REGISTERED` sobre `PRODUCT` |

### `Product updateProduct(User actor, String productId, String name, String description, Money price)`

- Solo el `SELLER` dueño (`ProductNotFoundException` /
  `UnauthorizedOperationException`). Aplica `changeDetails` y `changePrice`.
  Producto `DISCONTINUED` ⇒ `InvalidProductStateException`.
- **Auditoría:** `PRODUCT_UPDATED` sobre `PRODUCT`.

### `Product addVariant(User actor, String productId, VariantType type, String value)`

- Dueño del producto; `ProductVariant.of(type, value)`; `product.addVariant`.

### `Product publishProduct(User actor, String productId)`

- Dueño. `product.publish()` valida no descontinuado y, si es físico, que el
  vendedor tenga bodegas. **Además** el servicio exige
  `inventoryRepository.existsByProductId(productId)` para físicos (D-13); si no
  ⇒ `SellerWithoutWarehouseException`/`InvalidProductStateException`.
- **Auditoría:** `PRODUCT_PUBLISHED` sobre `PRODUCT`.

### `Product suspendProduct(User actor, String productId)`

- Dueño; `product.suspend()`; auditoría `PRODUCT_SUSPENDED`.

### `Product discontinueProduct(User actor, String productId)`

- Dueño; `product.discontinue()` (terminal); auditoría `PRODUCT_DISCONTINUED`.

### `List<Product> listPublishedProducts()` / `Product getPublishedProduct(String productId)`

- Público, sin actor. Solo devuelve productos `PUBLISHED`; si el producto no
  existe o no está publicado ⇒ `ProductNotFoundException` /
  `ProductNotPublishedException`.

### `List<Product> listBySeller(User actor, String sellerId)`

- `SELLER` propio o `ADMIN`; si no ⇒ `UnauthorizedOperationException`.

### `Product getProductForSeller(User actor, String productId)`

- Dueño o `ADMIN`.

## Reglas

- Un vendedor solo administra sus productos.
- Los productos digitales no requieren bodega ni inventario.
- Un producto descontinuado es terminal y no puede publicarse.
- El precio debe ser estrictamente mayor que cero.
