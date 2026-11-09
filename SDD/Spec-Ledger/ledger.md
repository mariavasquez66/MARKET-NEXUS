# Ledger de especificación — NexusMarket

> Fuente funcional exclusiva: `SDD/Especificación Funcional del Negocio - NexusMarket.pdf` (8 páginas).
> Cada fila es atómica y lleva etiqueta RVI. `[SUPUESTO]` no se implementa como regla obligatoria.
> Pasadas: `passes/pass-1.md` a `passes/pass-5.md`; la pasada 5 se ejecutó en F7.

## Cobertura RVI

- `[SPEC p.X §Y]`: afirmación explícita del PDF.
- `[DERIVADO de REQ-xxx]`: consecuencia técnica/funcional de un requisito verificado.
- `[SUPUESTO]`: no está en el PDF; requiere confirmación y no se trata como regla obligatoria.
- `[CONTRADICE]`: el código/documentación contradice el PDF; debe corregirse o justificarse.

## Ledger atómico

| REQ-id | Paráfrasis | Sección/página | Tipo | Dominio | OBJ | Etiqueta RVI | Cobertura |
|---|---|---|---|---|---|---|---|
| REQ-001 | NexusMarket intermedia entre compradores y vendedores y administra la operación completa. | p.1 §1 | Contexto | Plataforma | — | [SPEC p.1 §1] | CUBIERTO (contexto) |
| REQ-002 | Administrar la información de todos los usuarios. | p.1 §2 | Objetivo | Usuarios | OBJ-01 | [SPEC p.1 §2] | CUBIERTO: `UserService` |
| REQ-003 | Gestionar registro y administración de vendedores. | p.1 §2 | Objetivo | Vendedores | OBJ-02 | [SPEC p.1 §2] | CUBIERTO: `SellerService` |
| REQ-004 | Administrar compradores registrados. | p.1 §2 | Objetivo | Compradores | OBJ-03 | [SPEC p.1 §2] | CUBIERTO: `BuyerService` |
| REQ-005 | Controlar la información de las bodegas. | p.1 §2 | Objetivo | Bodegas | OBJ-04 | [SPEC p.1 §2] | CUBIERTO: `WarehouseService` |
| REQ-006 | Gestionar el catálogo de productos. | p.1 §2 | Objetivo | Catálogo | OBJ-05 | [SPEC p.1 §2] | CUBIERTO: `CatalogService` |
| REQ-007 | Administrar el inventario distribuido. | p.1 §2 | Objetivo | Inventario | OBJ-06 | [SPEC p.1 §2] | CUBIERTO: `InventoryService` |
| REQ-008 | Gestionar el carrito de compras. | p.1 §2 | Objetivo | Carrito | OBJ-07 | [SPEC p.1 §2] | CUBIERTO: `CartService` |
| REQ-009 | Controlar el ciclo completo de los pedidos. | p.1 §2 | Objetivo | Pedidos | OBJ-08 | [SPEC p.1 §2] | CUBIERTO: `OrderService` |
| REQ-010 | Administrar la facturación de las compras. | p.2 §2 | Objetivo | Facturación | OBJ-09 | [SPEC p.2 §2] | CUBIERTO: `InvoiceService` |
| REQ-011 | Gestionar los procesos logísticos. | p.2 §2 | Objetivo | Logística | OBJ-10 | [SPEC p.2 §2] | CUBIERTO: `LogisticsService` |
| REQ-012 | Administrar devoluciones y reembolsos. | p.2 §2 | Objetivo | Posventa | OBJ-11 | [SPEC p.2 §2] | CUBIERTO: `ReturnService`, `RefundService` |
| REQ-013 | Consolidar información administrativa para consulta. | p.2 §2 | Objetivo | Reportes | OBJ-12 | [SPEC p.2 §2] | CUBIERTO: `ReportService` |
| REQ-014 | El alcance incluye los 13 procesos listados en §3.1. | p.2-3 §3.1 | Alcance | Plataforma | — | [SPEC p.2-3 §3.1] | CUBIERTO (13 procesos) |
| REQ-015 | Interfaces, apps, portales, autenticación técnica, tecnología, arquitectura y almacenamiento quedan fuera del documento funcional. | p.3 §3.2 | Alcance | Plataforma | — | [SPEC p.3 §3.2] | CUBIERTO como [TECH] |
| REQ-016 | Componentes: usuarios, vendedores, compradores, bodegas, productos, inventario, pedidos, facturación y envíos. | p.3-4 §4.1 | Contexto | Plataforma | — | [SPEC p.3-4 §4.1] | CUBIERTO: modelos del dominio |
| REQ-017 | Participantes: Comprador, Vendedor, Operador Logístico, Administrador y Supervisor. | p.4 §5 | Actores | Usuarios | OBJ-01 | [SPEC p.4 §5] | CUBIERTO: `UserRole` |
| REQ-018 | Cada participante tiene un único rol y solo interactúa con información de sus funciones. | p.4 §5 | Regla | Usuarios | OBJ-01 | [SPEC p.4 §5] | CUBIERTO: `User.role`, RG-02/03 |
| REQ-019 | Flujo: incorporación, catálogo, inventario, publicación, compra, transacción, logística y cierre. | p.4 §6.1 | Proceso | Plataforma | — | [SPEC p.4 §6.1] | PARCIAL: cubierto por servicios, sin proceso orquestador único |
| REQ-020 | El Administrador registra al vendedor y su primera bodega. | p.4 §6.1.1 | Proceso | Vendedores/Bodegas | OBJ-02/04 | [SPEC p.4 §6.1.1] | CUBIERTO: `SellerService.registerSeller`, `WarehouseService.createWarehouse` |
| REQ-021 | El vendedor registra productos y define sus características. | p.4 §6.1.2 | Proceso | Catálogo | OBJ-05 | [SPEC p.4 §6.1.2] | CUBIERTO: `CatalogService.createProduct` |
| REQ-022 | Se registran existencias iniciales en las bodegas asociadas. | p.4 §6.1.3 | Proceso | Inventario | OBJ-06 | [SPEC p.4 §6.1.3] | CUBIERTO: `InventoryService.registerInitialStock`; rol ejecutor [SUPUESTO] |
| REQ-023 | Los productos se publican en el catálogo público. | p.4 §6.1.4 | Proceso | Catálogo | OBJ-05 | [SPEC p.4 §6.1.4] | CUBIERTO: `CatalogService.publish` |
| REQ-024 | El comprador selecciona productos en el carrito y confirma el pedido. | p.4 §6.1.5 | Proceso | Carrito/Pedidos | OBJ-07/08 | [SPEC p.4 §6.1.5] | CUBIERTO: `CartService`, `OrderService.checkout` |
| REQ-025 | Se valida el pago y se inicia la preparación. | p.4 §6.1.6 | Proceso | Pedidos | OBJ-08 | [SPEC p.4 §6.1.6] | CUBIERTO: `OrderService.confirmPayment` |
| REQ-026 | Se realiza empaque, despacho y transporte del pedido. | p.4 §6.1.7 | Proceso | Logística | OBJ-10 | [SPEC p.4 §6.1.7] | CUBIERTO: `LogisticsService` |
| REQ-027 | El pedido se finaliza tras la entrega confirmada. | p.4 §6.1.8 | Proceso | Pedidos | OBJ-08 | [SPEC p.4 §6.1.8] | CUBIERTO: `OrderService.finalize` |
| REQ-028 | Usuario: identificador obligatorio y único. | p.5 Dominio 1 | Atributo | Usuarios | OBJ-01 | [SPEC p.5 Dominio 1] | CUBIERTO: `User.id` |
| REQ-029 | Usuario: nombre completo obligatorio y no vacío. | p.5 Dominio 1 | Atributo | Usuarios | OBJ-01 | [SPEC p.5 Dominio 1] | CUBIERTO: `FullName` |
| REQ-030 | Usuario: correo obligatorio, único y medio principal de acceso/comunicación. | p.5 Dominio 1 | Atributo | Usuarios | OBJ-01 | [SPEC p.5 Dominio 1] | CUBIERTO: `Email`, unicidad |
| REQ-031 | Usuario: rol obligatorio y único por usuario. | p.5 Dominio 1 | Atributo | Usuarios | OBJ-01 | [SPEC p.5 Dominio 1] | CUBIERTO: `UserRole`, `User.role` |
| REQ-032 | Usuario: estado obligatorio con catálogo definido (Activo, Bloqueado, etc.). | p.5 Dominio 1 | Atributo | Usuarios | OBJ-01 | [SPEC p.5 Dominio 1] | CUBIERTO: `UserStatus`; valores completos [SUPUESTO] |
| REQ-033 | Comprador: dirección principal obligatoria. | p.6 Dominio 2 | Atributo | Compradores | OBJ-03 | [SPEC p.6 Dominio 2] | CUBIERTO: `Buyer.mainAddress` |
| REQ-034 | Comprador: direcciones adicionales opcionales. | p.6 Dominio 2 | Atributo | Compradores | OBJ-03 | [SPEC p.6 Dominio 2] | CUBIERTO: `Buyer.additionalAddresses` |
| REQ-035 | Comprador: estado comercial obligatorio y condición para comprar. | p.6 Dominio 2 | Atributo/Regla | Compradores | OBJ-03 | [SPEC p.6 Dominio 2] | CUBIERTO: `CommercialStatus`, `assertCanPurchase` |
| REQ-036 | El comprador no administra información de otros compradores ni inventarios. | p.6 Dominio 2 | Restricción | Compradores | OBJ-03 | [SPEC p.6 Dominio 2] | CUBIERTO: servicios por actor; auditoría de IDOR en F3 |
| REQ-037 | Los vendedores no pueden autorregistrarse; los incorpora el Administrador. | p.6 Dominio 3 | Regla | Vendedores | OBJ-02 | [SPEC p.6 Dominio 3] | CUBIERTO: `SellerService.registerSeller` |
| REQ-038 | Bodegas: espacios físicos de almacenamiento. | p.6 Dominio 4 | Atributo | Bodegas | OBJ-04 | [SPEC p.6 Dominio 4] | CUBIERTO: `Warehouse` |
| REQ-039 | Bodegas: se distinguen bodegas del Marketplace y bodegas de Vendedores. | p.6 Dominio 4 | Catálogo | Bodegas | OBJ-04 | [SPEC p.6 Dominio 4] | CUBIERTO: `WarehouseType` |
| REQ-040 | Producto: tipo Físico o Digital. | p.6-7 Dominio 5 | Catálogo | Catálogo | OBJ-05 | [SPEC p.6-7 Dominio 5] | CUBIERTO: `ProductType` |
| REQ-041 | Producto físico: requiere inventario y despacho. | p.6 Dominio 5 | Regla | Catálogo/Inventario | OBJ-05/06 | [SPEC p.6 Dominio 5] | CUBIERTO: `Product.requiresInventory/requiresShipment` |
| REQ-042 | Producto digital: entrega inmediata tras el pago. | p.6 Dominio 5 | Regla | Catálogo/Pedidos | OBJ-05/08 | [SPEC p.6 Dominio 5] | CUBIERTO: `OrderService.confirmPayment` |
| REQ-043 | Producto: variantes como lista (color, talla, modelo, etc.). | p.7 Dominio 5 | Atributo | Catálogo | OBJ-05 | [SPEC p.7 Dominio 5] | CUBIERTO: `ProductVariant` |
| REQ-044 | Producto: estado Publicado, Suspendido o Descontinuado. | p.7 Dominio 5 | Catálogo | Catálogo | OBJ-05 | [SPEC p.7 Dominio 5] | CUBIERTO: `ProductStatus` |
| REQ-045 | Inventario distribuido, vinculado obligatoriamente a producto y bodega. | p.7 Dominio 6 | Regla | Inventario | OBJ-06 | [SPEC p.7 Dominio 6] | CUBIERTO: `Inventory` |
| REQ-046 | Movimientos: Ingreso, Reserva, Salida por venta, Ajuste y Devolución. | p.7 Dominio 6 | Catálogo | Inventario | OBJ-06 | [SPEC p.7 Dominio 6] | CUBIERTO: `MovementType` |
| REQ-047 | No se permiten existencias negativas bajo ninguna circunstancia. | p.7 Dominio 6 | Invariante | Inventario | OBJ-06 | [SPEC p.7 Dominio 6] | CUBIERTO: `Inventory` |
| REQ-048 | No se puede reservar inventario inexistente. | p.8 §11 | Validación | Inventario | OBJ-06 | [SPEC p.8 §11] | CUBIERTO: `InsufficientStockException` |
| REQ-049 | No se puede reservar inventario marcado como Dañado. | p.8 §11 | Validación | Inventario | OBJ-06 | [SPEC p.8 §11] | CUBIERTO: `DamagedInventoryException` |
| REQ-050 | El pedido es el compromiso comercial formal. | p.7 Dominio 7 | Contexto | Pedidos | OBJ-08 | [SPEC p.7 Dominio 7] | CUBIERTO: `Order` |
| REQ-051 | Ciclo: Carrito → Pendiente de Pago → Pagado → Despachado → Entregado/Finalizado. | p.7 Dominio 7 | Estados | Pedidos | OBJ-08 | [SPEC p.7 Dominio 7] | PARCIAL: `OrderStatus`; `CART` no se usa y hay `CANCELLED` adicional [SUPUESTO] |
| REQ-052 | Un pedido finalizado no puede modificarse bajo ninguna circunstancia. | p.8 §11 | Invariante | Pedidos | OBJ-08 | [SPEC p.8 §11] | CUBIERTO: `Order.assertModifiable` |
| REQ-053 | Toda operación debe ejecutarse por un usuario autenticado. | p.7 §10 RG-01 | Restricción | Seguridad | — | [SPEC p.7 §10 RG-01] | CUBIERTO: actor + JWT [TECH] |
| REQ-054 | Cada usuario tendrá un único rol. | p.7 §10 RG-02 | Restricción | Usuarios | OBJ-01 | [SPEC p.7 §10 RG-02] | CUBIERTO: `User.role` |
| REQ-055 | Ningún participante administra información fuera de su rol. | p.8 §10 RG-03 | Restricción | Seguridad | — | [SPEC p.8 §10 RG-03] | PARCIAL: auditoría de autorización en F3 |
| REQ-056 | Documento de identidad y correo deben ser únicos en la plataforma. | p.8 §11 | Validación | Usuarios | OBJ-01 | [SPEC p.8 §11] | CUBIERTO: servicios + índices únicos |
| REQ-057 | Matriz: Registro de Vendedores → Admin. | p.8 §12 | Responsabilidad | Vendedores | OBJ-02 | [SPEC p.8 §12] | CUBIERTO: `TellerEmployeePort` |
| REQ-058 | Matriz: Registro de Productos → Vendedor. | p.8 §12 | Responsabilidad | Catálogo | OBJ-05 | [SPEC p.8 §12] | CUBIERTO: `SellerController` |
| REQ-059 | Matriz: Administración de Inventario → Vendedor + Op. Logístico. | p.8 §12 | Responsabilidad | Inventario | OBJ-06 | [SPEC p.8 §12] | CUBIERTO: endpoints por rol; lectura de vendedor validada sobre toda la colección en F3 |
| REQ-060 | Matriz: Gestión de Pedidos → Comprador + Vendedor + Op. Logístico. | p.8 §12 | Responsabilidad | Pedidos | OBJ-08 | [SPEC p.8 §12] | CUBIERTO: endpoints por rol |
| REQ-061 | Matriz: Gestión de Reembolsos → Comprador + Admin. | p.8 §12 | Responsabilidad | Reembolsos | OBJ-11 | [SPEC p.8 §12] | PARCIAL: comprador solo consulta; Admin genera/procesa |
| REQ-062 | La facturación está incluida en el alcance, sin detalle funcional. | p.2 §3.1 | Alcance | Facturación | OBJ-09 | [SPEC p.2 §3.1] | PARCIAL: `InvoiceService` existe; reglas fiscales [SUPUESTO] |
| REQ-063 | Los envíos están incluidos en el alcance, sin detalle funcional. | p.2 §3.1 | Alcance | Logística | OBJ-10 | [SPEC p.2 §3.1] | PARCIAL: `Shipment` y `LogisticsService`; reglas de agrupación [SUPUESTO] |
| REQ-064 | Las devoluciones están incluidas en el alcance, sin detalle funcional. | p.2 §3.1 | Alcance | Posventa | OBJ-11 | [SPEC p.2 §3.1] | PARCIAL: `ReturnService`; aprobador [SUPUESTO] |
| REQ-065 | Los reembolsos están incluidos en el alcance, sin detalle funcional. | p.2 §3.1 | Alcance | Posventa | OBJ-11 | [SPEC p.2 §3.1] | PARCIAL: `RefundService`; reglas de monto [SUPUESTO] |
| REQ-066 | La consulta de reportes administrativos está incluida, sin detalle funcional. | p.3 §3.1 | Alcance | Reportes | OBJ-12 | [SPEC p.3 §3.1] | PARCIAL: `ReportService`; contenido/rol [SUPUESTO] |
| REQ-067 | No hay requisito de IVA, tasa 19% ni precios con IVA incluido en el PDF. | — | Supuesto | Facturación | OBJ-09 | [SUPUESTO] | IMPLEMENTADO POR D-12; no es requisito verificado |
| REQ-068 | No hay requisito de cancelación de pedidos en el PDF. | — | Supuesto | Pedidos | OBJ-08 | [SUPUESTO] | IMPLEMENTADO: `OrderStatus.CANCELLED`; no es requisito verificado |
| REQ-069 | No hay requisito de un envío por bodega ni agrupación logística en el PDF. | — | Supuesto | Logística | OBJ-10 | [SUPUESTO] | NO VERIFICADO; revisar F3 |
| REQ-070 | “Entregado / Finalizado” aparece como un solo paso; no se exige separar dos estados. | p.7 Dominio 7 | Interpretación | Pedidos | OBJ-08 | [DERIVADO de REQ-051] | CÓDIGO SEPARA `DELIVERED` y `FINALIZED`; decisión derivada, no contradicción explícita |
| REQ-071 | No hay catálogo explícito de estados comerciales del comprador. | — | Supuesto | Compradores | OBJ-03 | [SUPUESTO] | IMPLEMENTADO: `ELIGIBLE/RESTRICTED/SUSPENDED`; no verificado |
| REQ-072 | No se asigna la consulta de reportes al Supervisor en la matriz. | p.4 §5 + p.8 §12 | Supuesto | Reportes | OBJ-12 | [SUPUESTO] | IMPLEMENTADO: solo `SUPERVISOR`; no verificado |
| REQ-073 | No se define quién aprueba devoluciones ni quién procesa reembolsos. | p.2 §3.1 | Supuesto | Posventa | OBJ-11 | [SUPUESTO] | IMPLEMENTADO: Admin; no verificado |
| REQ-074 | No hay catálogo completo de estados de usuario; el PDF usa “etc.”. | p.5 Dominio 1 | Supuesto | Usuarios | OBJ-01 | [SUPUESTO] | IMPLEMENTADO: `ACTIVE/BLOCKED/INACTIVE`; `INACTIVE` no verificado |
| REQ-075 | El PDF usa Carrito como paso del ciclo y también como proceso/objetivo propio. | p.1 §2 + p.7 Dominio 7 | Interpretación | Carrito/Pedidos | OBJ-07/08 | [DERIVADO de REQ-008/051] | CÓDIGO: `Cart` separado; `OrderStatus.CART` sin uso |
| REQ-076 | Los movimientos de inventario son cinco y sus nombres funcionales están definidos. | p.7 Dominio 6 | Catálogo | Inventario | OBJ-06 | [SPEC p.7 Dominio 6] | CUBIERTO: `MovementType` con 5 valores |
| REQ-077 | El PDF no indica qué rol registra las existencias iniciales. | p.4 §6.1.3 | Supuesto | Inventario | OBJ-06 | [SUPUESTO] | IMPLEMENTADO: `SELLER`; no verificado |
| REQ-078 | El PDF no define `TaxId`, propietario ni responsable de bodega. | p.6 Dominio 3-4 | Supuesto | Vendedores/Bodegas | OBJ-02/04 | [SUPUESTO] | IMPLEMENTADO POR D-05; no verificado |
| REQ-079 | El PDF no define el contenido de reportes ni reglas de reembolso parcial. | p.2 §2 + p.3 §3.1 | Supuesto | Reportes/Posventa | OBJ-11/12 | [SUPUESTO] | IMPLEMENTADO POR D-15/ReportService; no verificado |
| REQ-080 | La autenticación técnica, JWT, BCrypt y CORS no son requisitos del PDF. | p.3 §3.2 | Técnico | Seguridad | — | [TECH] | CUBIERTO por SDD/prompt |
| REQ-081 | La arquitectura hexagonal, paquetes y capas no son requisitos del PDF. | p.3 §3.2 | Técnico | Plataforma | — | [TECH] | CUBIERTO por SDD/prompt |
| REQ-082 | La persistencia JPA/MongoDB, Docker y perfiles no son requisitos del PDF. | p.3 §3.2 | Técnico | Infraestructura | — | [TECH] | CUBIERTO por SDD/prompt |
| REQ-083 | La auditoría en MongoDB no es un requisito del PDF. | p.3 §3.2 | Técnico | Auditoría | — | [TECH] | CUBIERTO por SDD/prompt |
| REQ-084 | La forma REST, códigos HTTP y DTOs no son requisitos del PDF. | p.3 §3.2 | Técnico | API | — | [TECH] | CUBIERTO por SDD/prompt |

## Validación explícita solicitada

| Punto | Resultado | Etiqueta RVI |
|---|---|---|
| (a) Rol de cada ✔ de la matriz | Recuperado por coordenadas: Admin, Seller, Seller+Op. Log., Buyer+Seller+Op. Log., Buyer+Admin. | [SPEC p.8 §12] |
| (b) IVA 19% / precios con IVA | No aparece en el PDF. | [SUPUESTO] |
| (c) Cancelación solo antes de pago | No aparece en el PDF; el código solo permite cancelar `PENDING_PAYMENT`. | [SUPUESTO] |
| (d) Un envío por bodega | No aparece en el PDF. | [SUPUESTO] |
| (e) `DELIVERED`/`FINALIZED` separados | El PDF los presenta como un paso; el código los separa. | [DERIVADO de REQ-051] |
| (f) `CommercialStatus` | El PDF exige estado comercial, pero no sus valores. | [SUPUESTO] |
| (g) Reportes solo `SUPERVISOR` | La matriz no asigna reportes; el código restringe a Supervisor. | [SUPUESTO] |
| (h) Aprobación de devoluciones / reembolsos | El PDF solo asigna reembolsos a Comprador+Admin; no define aprobación de devoluciones. | [SPEC p.8 §12] + [SUPUESTO] |
| (i) Estados de usuario | El PDF solo ejemplifica Activo/Bloqueado y deja “etc.”. | [SPEC p.5 Dominio 1] + [SUPUESTO] |
| (j) Carrito separado del pedido | El PDF lo lista como paso y como proceso/objetivo; el código lo separa. | [DERIVADO de REQ-008/051] |
| (k) Movimientos de inventario | Los cinco movimientos están explícitos. | [SPEC p.7 Dominio 6] |
| (l) Quién registra existencias iniciales | El PDF no lo indica. | [SUPUESTO] |

## Supuestos abiertos

Ver `SDD/Supuestos-no-verificados.md`. No se implementarán como reglas obligatorias hasta confirmación humana.
