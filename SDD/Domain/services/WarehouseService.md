# `WarehouseService`

**OBJ:** OBJ-04 · **Acceso:** `ADMIN` (crear/actualizar), lectura autorizada
**Paquete:** `application.domain.services.WarehouseService`

## Dependencias (constructor)

`WarehouseRepositoryPort`, `SellerRepositoryPort`, `UserRepositoryPort`,
`AuditPort`, `ClockPort`.

## Métodos

### `Warehouse createWarehouse(User actor, String name, Address location, WarehouseType type, String ownerSellerId, String responsibleUserId)`

| Elemento | Detalle |
|---|---|
| Precondiciones | actor `ADMIN` (`InsufficientPermissionException`); nombre y dirección válidos; responsable existe y está `ACTIVE` |
| Flujo | 1) Carga el responsable (`UserNotFoundException`) y valida rol `ADMIN` o `LOGISTIC_OPERATOR` (`InvalidRoleException`). 2) Si `type == SELLER`: exige `ownerSellerId`, carga el vendedor (`SellerNotFoundException`) y usa `createSellerWarehouse(...)`. 3) Si `type == MARKETPLACE`: `ownerSellerId` debe ser nulo/ vacío y usa `createMarketplace(...)`. 4) Guarda. |
| Excepciones | `InvalidWarehouseTypeException` (tipo/dueño incoherente), `InvalidRoleException`, `SellerNotFoundException`, `UserNotFoundException` |
| Auditoría | `WAREHOUSE_CREATED` sobre `WAREHOUSE` |

### `Warehouse updateWarehouse(User actor, String warehouseId, String name, Address location, String responsibleUserId)`

- Solo `ADMIN`. Carga bodega (`WarehouseNotFoundException`) y responsable
  (`UserNotFoundException`); valida rol; aplica `changeName`, `changeLocation`,
  `assignResponsible`; guarda.
- **Auditoría:** `WAREHOUSE_UPDATED` sobre `WAREHOUSE`.

### `List<Warehouse> listWarehouses(User actor)`

- Permitido a `ADMIN`, `SUPERVISOR` y `LOGISTIC_OPERATOR`; un `SELLER` solo ve
  las suyas (delegado a `listBySeller`).

### `List<Warehouse> listBySeller(User actor, String sellerId)`

- `SELLER` solo si `actor.id == seller.user.id`; `ADMIN`/`SUPERVISOR` cualquiera.
  Si no ⇒ `UnauthorizedOperationException`.

### `Warehouse getWarehouse(User actor, String warehouseId)`

- Mismas reglas de visibilidad que `listWarehouses`/`listBySeller` según el rol.

## Reglas

- Bodega `SELLER` ⇒ dueño obligatorio y de tipo vendedor.
- Bodega `MARKETPLACE` ⇒ sin dueño.
- Toda bodega requiere responsable con rol `ADMIN` o `LOGISTIC_OPERATOR`.
