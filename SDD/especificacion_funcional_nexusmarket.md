# Especificación funcional — NexusMarket

> Transcripción del Anexo A del prompt maestro, que a su vez transcribe el PDF
> `SDD/Especificación Funcional del Negocio - NexusMarket.pdf`.
> Esta especificación es la **fuente de verdad nº 1** del proyecto.
> El código Java se escribe en inglés; la documentación se mantiene en español.

## A.1 Contexto

NexusMarket es una plataforma digital centralizada que actúa como intermediario
comercial entre compradores y vendedores. Administra la operación completa:
registro de usuarios, publicación de productos, logística, facturación y
posventa, con trazabilidad y coordinación entre todos los participantes.

## A.2 Objetivos

| Código | Objetivo funcional | Servicio(s) que lo cubren |
|---|---|---|
| OBJ-01 | Administrar la información de todos los usuarios del Marketplace | `UserService`, `AuthService` |
| OBJ-02 | Gestionar el registro y administración de vendedores | `SellerService` |
| OBJ-03 | Administrar compradores registrados | `BuyerService`, `AuthService` |
| OBJ-04 | Controlar la información de las bodegas | `WarehouseService` |
| OBJ-05 | Gestionar el catálogo de productos | `CatalogService` |
| OBJ-06 | Administrar el inventario distribuido | `InventoryService` |
| OBJ-07 | Gestionar el carrito de compras | `CartService` |
| OBJ-08 | Controlar el ciclo completo de los pedidos | `OrderService` |
| OBJ-09 | Administrar la facturación de las compras | `InvoiceService` |
| OBJ-10 | Gestionar los procesos logísticos | `LogisticsService` |
| OBJ-11 | Administrar devoluciones y reembolsos | `ReturnService`, `RefundService` |
| OBJ-12 | Consolidar información administrativa para consulta | `ReportService` |

## A.3 Alcance

**Incluido:** registro de compradores, administración de usuarios, registro
administrativo de vendedores, administración de bodegas, gestión del catálogo,
administración del inventario, gestión del carrito, gestión de pedidos,
facturación, envíos, devoluciones, reembolsos y consulta de reportes
administrativos.

**Fuera del alcance (del documento funcional):** interfaces gráficas,
aplicaciones móviles, portales web, mecanismos de autenticación técnica,
tecnologías de implementación, arquitectura del software y almacenamiento de la
información. *(La implementación técnica se rige por el SDD y el prompt maestro.)*

## A.4 Componentes del negocio

Usuarios (personas autorizadas) · Vendedores (comercializan productos) ·
Compradores (realizan compras) · Bodegas (inventario físico) · Productos (bienes
físicos o digitales) · Inventario (existencias disponibles) · Pedidos
(solicitudes de compra) · Facturación (información comercial de ventas) ·
Envíos (logística de productos físicos).

## A.5 Participantes

Cada participante desempeña **un único rol** y solo interactúa con la información
de sus funciones.

| Participante | Rol (`UserRole`) | Descripción |
|---|---|---|
| Comprador | `BUYER` | Adquiere productos publicados |
| Vendedor | `SELLER` | Registra y administra sus productos |
| Operador Logístico | `LOGISTIC_OPERATOR` | Operación física de bodegas y despachos |
| Administrador | `ADMIN` | Administración de vendedores y bodegas |
| Supervisor | `SUPERVISOR` | Perfil de consulta y seguimiento operativo |

## A.6 Flujo general del negocio

1. **Incorporación:** el Administrador registra al vendedor y su primera bodega.
2. **Catálogo:** el vendedor registra productos y define sus características.
3. **Inventario:** se registran existencias iniciales en las bodegas asociadas.
4. **Publicación:** los productos se hacen visibles en el catálogo público.
5. **Compra:** el comprador selecciona productos mediante el carrito y confirma
   el pedido.
6. **Transacción:** se valida el pago y se inicia el flujo de preparación.
7. **Logística:** empaque, despacho y transporte del pedido.
8. **Cierre:** el pedido se marca como finalizado tras la entrega confirmada.

## A.7 Dominios funcionales

### Dominio 1 — Administración de Usuarios (todos los roles)

Atributos: Identificador (obligatorio, único) · Nombre completo (obligatorio, no
vacío) · Correo electrónico (obligatorio, único; medio principal de acceso y
comunicación) · Rol (obligatorio, único por usuario) · Estado (obligatorio,
catálogo definido: Activo, Bloqueado, etc.).

*Validación crítica:* documento de identidad y correo únicos.

Modelo: `User` con `FullName`, `Email`, `IdentityDocument`, `UserRole`,
`UserStatus`, `PasswordHash` y `authTokenVersion`.

### Dominio 2 — Gestión de Compradores

Atributos: Dirección principal (obligatorio) · Direcciones adicionales
(opcional) · Estado comercial (obligatorio; condición para realizar compras).

*Restricción:* el comprador nunca administra información de otros compradores ni
inventarios.

Modelo: `Buyer` con `Address`, `CommercialStatus` (`ELIGIBLE`, `RESTRICTED`,
`SUSPENDED`).

### Dominio 3 — Gestión de Vendedores

Los vendedores **no pueden autorregistrarse**; los incorpora el Administrador.

Modelo: `Seller` con `User`, `TaxId` y `warehouses`. La operación
`Seller.registerBy(adminActor, sellerUser, taxId)` exige rol `ADMIN`.

### Dominio 4 — Gestión de Bodegas

Controla espacios físicos de almacenamiento; se distinguen **bodegas del
Marketplace** y **bodegas de Vendedores**.

Modelo: `Warehouse` con `WarehouseType` (`MARKETPLACE`, `SELLER`), propietario
vendedor (obligatorio para `SELLER`, nulo para `MARKETPLACE`) y responsable
físico (`ADMIN` o `LOGISTIC_OPERATOR`).

### Dominio 5 — Gestión del Catálogo

Productos **físicos** (requieren inventario y despacho) y **digitales** (entrega
inmediata tras el pago). Atributos: Tipo de producto (Físico/Digital) · Variantes
(lista: color, talla, modelo…) · Estado (Publicado, Suspendido, Descontinuado).

Modelo: `Product` con `ProductType`, `ProductStatus`, `ProductVariant` y `Money`.

### Dominio 6 — Gestión del Inventario

Distribuido; vinculado obligatoriamente a un producto y a una bodega.
Movimientos: Ingreso, Reserva, Salida por venta, Ajuste y Devolución.

*Restricción:* no se permiten existencias negativas bajo ninguna circunstancia.

Modelo: `Inventory` (`availableQuantity`, `reservedQuantity`,
`InventoryCondition`) e `InventoryMovement` (`MovementType`).

### Dominio 7 — Gestión de Pedidos

Compromiso comercial formal; su ciclo de vida es el proceso central. Estados:
1) Carrito (selección provisional) → 2) Pendiente de Pago → 3) Pagado (inicio de
alistamiento) → 4) Despachado (salida física de bodega) → 5)
Entregado/Finalizado.

Modelo: `Cart`/`CartItem` y `Order` con `OrderStatus`, `PaymentStatus`, `Money`,
`StockAllocation`, `Invoice`, `Shipment`, `Return` y `Refund`.

## A.8 Restricciones generales

| Código | Restricción | Implementación prevista |
|---|---|---|
| RG-01 | Toda operación debe ejecutarse por un usuario autenticado | `User` actor como primer parámetro de cada puerto de entrada (salvo público) |
| RG-02 | Cada usuario tendrá un único rol dentro del sistema | `User.role` inmutable; `UserRole` único |
| RG-03 | Ningún participante podrá administrar información fuera de su rol | Validación de rol en cada servicio de dominio |

## A.9 Validaciones críticas

- **Inventario:** no se puede reservar inventario inexistente o marcado como
  "Dañado". → `InsufficientStockException`, `DamagedInventoryException`,
  `NegativeStockException`.
- **Pedidos:** un pedido finalizado no podrá ser modificado bajo ninguna
  circunstancia. → `FinalizedOrderException`, `CancelledOrderException`.
- **Usuarios:** el documento de identidad y el correo electrónico deben ser
  únicos en la plataforma. → `DuplicateEmailException`,
  `DuplicateIdentificationException`.
- **Vendedores:** solo el Administrador puede registrarlos. →
  `SelfRegistrationNotAllowedException`.

## A.10 Matriz de responsabilidades

> ⚠ **Transcripción con columnas perdidas en el PDF.** La asignación siguiente
> es la propuesta D-09 del prompt maestro y queda registrada como **supuesto**
> hasta que la persona dueña del proyecto la confirme.

| Proceso | Comprador | Vendedor | Op. Logístico | Administrador | Supervisor |
|---|:---:|:---:|:---:|:---:|:---:|
| Registro de vendedores | | | | ✔ | |
| Registro de productos | | ✔ | | | |
| Administración de inventario | | ✔ (alta/consulta propia) | ✔ (movimientos/daños/reingresos) | | |
| Gestión de pedidos | ✔ (carrito/checkout/pago/cancelar) | ✔ (consulta con sus productos) | ✔ (envío/entrega) | | |
| Devoluciones | ✔ (solicita) | | ✔ (recibe) | ✔ (aprueba/rechaza) | |
| Reembolsos | ✔ (consulta) | | | ✔ (genera/procesa) | |
| Reportes | | | | | ✔ (solo lectura) |

## A.11 Glosario de equivalencias

| Documento funcional | Código (`DomainCatalog`) |
|---|---|
| Activo (usuario) | `UserStatus.ACTIVE` |
| Bloqueado | `UserStatus.BLOCKED` |
| Baja lógica | `UserStatus.INACTIVE` |
| Habilitado comercialmente | `CommercialStatus.ELIGIBLE` |
| Restringido / Suspendido | `CommercialStatus.RESTRICTED` / `SUSPENDED` |
| Físico / Digital | `ProductType.PHYSICAL` / `DIGITAL` |
| Publicado / Suspendido / Descontinuado | `ProductStatus.PUBLISHED` / `SUSPENDED` / `DISCONTINUED` |
| Carrito / Pendiente de pago / Pagado | `OrderStatus.CART` / `PENDING_PAYMENT` / `PAID` |
| Despachado (`Shipped`) | `OrderStatus.DISPATCHED` |
| Entregado / Finalizado | `OrderStatus.DELIVERED` / `FINALIZED` |
| Bodega Marketplace / Bodega Vendedor | `WarehouseType.MARKETPLACE` / `SELLER` |
| Ingreso / Reserva / Venta / Ajuste / Devolución | `MovementType.INFLOW` / `RESERVATION` / `SALE` / `ADJUSTMENT` / `RETURN` |
