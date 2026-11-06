# `CatalogQueryService`

**OBJ:** OBJ-05 — **Acceso:** público, `SELLER`, `ADMIN`
**Paquete:** `application.domain.services.CatalogQueryService`

## Trazabilidad

| REQ | Sección del PDF | Regla cubierta |
|---|---|---|
| REQ-006 | p.1 §2 | Gestionar el catálogo de productos. |
| REQ-021 | p.4 §6.1.2 | El vendedor registra productos y características. |
| REQ-023 | p.4 §6.1.4 | Los productos se publican en el catálogo público. |
| REQ-058 | p.8 §12 | Registro de productos por vendedor. |

## Responsabilidad

Resuelve consultas del catálogo público, del catálogo propio del vendedor y de
productos por vendedor para administración. Es invocado por `CatalogService`.

## Reglas

- El catálogo público solo devuelve productos publicados y comprables.
- Un producto no publicado produce `ProductNotPublishedException`.
- `listMyProducts` exige `SELLER` y resuelve el vendedor desde el usuario.
- `listBySeller` permite `SELLER` dueño o `ADMIN`.
- `getProductForSeller` permite `SELLER` dueño o `ADMIN`; otro vendedor produce
  `UnauthorizedOperationException`.

## Pruebas

`CatalogQueryServiceTest`: rechazo de producto no publicado y lectura de producto
propio con rechazo de vendedor ajeno.
