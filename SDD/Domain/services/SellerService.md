# `SellerService`

**OBJ:** OBJ-02 · **Acceso:** `ADMIN` (alta y administración), consulta propia/rol autorizado
**Paquete:** `application.domain.services.SellerService`

## Dependencias (constructor)

`SellerRepositoryPort`, `UserRepositoryPort`, `WarehouseRepositoryPort`,
`AuditPort`, `ClockPort`.

## Métodos

### `Seller registerSeller(User actor, String userId, TaxId taxId)`

| Elemento | Detalle |
|---|---|
| Precondiciones | `taxId` válido; usuario existe (`UserNotFoundException`) y tiene rol `SELLER` (`InvalidRoleException`) |
| Flujo | 1) `Seller.registerBy(actor, sellerUser, taxId)`: si el actor no es `ADMIN` ⇒ `SelfRegistrationNotAllowedException` (R-08). 2) Verifica `sellerRepository.existsByTaxId(taxId)` ⇒ `DuplicateEntityException`. 3) Guarda el vendedor. |
| Auditoría | `SELLER_REGISTERED` sobre `SELLER` |

### `Seller assignWarehouse(User actor, String sellerId, String warehouseId)`

- Solo `ADMIN`. Carga vendedor (`SellerNotFoundException`) y bodega
  (`WarehouseNotFoundException`). `seller.assignWarehouse(warehouse)` valida
  tipo `SELLER`, propiedad y no duplicado
  (`InvalidWarehouseTypeException`, `WarehouseAlreadyAssignedException`).
  Guarda vendedor y bodega.
- **Auditoría:** `WAREHOUSE_ASSIGNED` sobre `WAREHOUSE`.

### `Seller getSeller(User actor, String sellerId)`

- Permitido a `ADMIN`, `SUPERVISOR` o al propio vendedor
  (`seller.user.id == actor.id`); si no ⇒ `UnauthorizedOperationException`.

### `List<Seller> listSellers(User actor)`

- Solo `ADMIN` o `SUPERVISOR` (lectura).

### `Seller updateTaxId(User actor, String sellerId, TaxId taxId)`

- Solo `ADMIN`. Verifica unicidad (`DuplicateEntityException`), aplica
  `seller.updateTaxId(taxId)` y guarda.
- **Auditoría:** `SELLER_TAX_ID_UPDATED` sobre `SELLER`.

## Reglas

- Un vendedor **nunca** se autorregistra (R-08).
- Un vendedor solo puede tener bodegas de tipo `SELLER` y propias.
- El `User` del vendedor es inmutable.
