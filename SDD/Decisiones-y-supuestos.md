# Decisiones de diseño y supuestos

> Fuente: sección 5 del prompt maestro (D-01…D-17) más los supuestos nuevos
> detectados al escribir el SDD. Si una decisión se confirma o cambia, se
> actualiza este documento y, si afecta al código, se abre un commit aparte.

## Decisiones del prompt maestro

| ID | Decisión | Consecuencia en el código |
|---|---|---|
| D-01 | Paquete raíz `application`. Capas `domain/{models,enums,valueobjects,services,exceptions,ports/{in,out}}`, `adapters/{rest,useCases,persistence/{jpa,mongodb}}`, `infrastructure/{config,security}`. | Todo el código vive bajo `src/main/java/application/...`; no hay paquetes fuera de `application`. |
| D-02 | Se conserva el patrón `DomainCatalog` (clase con código/descripción, `fromCode`, `values`). | No se usa `enum` de Java; JPA requiere `AttributeConverter` por catálogo. |
| D-03 | `OrderStatus = CART, PENDING_PAYMENT, PAID, DISPATCHED, DELIVERED, FINALIZED, CANCELLED`. | `CART` se conserva por trazabilidad, pero el carrito real es el agregado `Cart`. |
| D-04 | `CommercialStatus = ELIGIBLE, RESTRICTED, SUSPENDED`; `UserStatus = ACTIVE, BLOCKED, INACTIVE`. | Se renombra `ACTIVE → ELIGIBLE` y se añade `SUSPENDED`; `INACTIVE` es baja lógica (sin flag `deleted`). |
| D-05 | `Cart`/`CartItem` como agregado; `Order.fromCart(...)` nace en `PENDING_PAYMENT` y contiene `List<CartItem>`. | `CartItem` no tiene repositorio propio. |
| D-06 | Enums/VOs adicionales obligatorios: `WarehouseType`, `MovementType`, `InventoryCondition`, `InvoiceStatus`, `RefundStatus`, `VariantType`, `AuditAction`, `Money`, `Address`, `Email`, `FullName`, `IdentityDocument`, `TaxId`, `ProductVariant`, `PasswordHash`, `StockAllocation`. | Reemplazan booleanos (`damaged`, `voided`, `processed`) y `String` acotados. |
| D-07 | Semántica de inventario: `availableQuantity` libres, `reservedQuantity` retenidas; `reserve`, `confirmSale`, `releaseReservation`, `adjust`, `registerReturn`; todo movimiento genera `InventoryMovement`; productos digitales sin inventario. | `Inventory` nunca queda negativo y `DAMAGED` no reserva ni vende. |
| D-08 | Checkout reserva stock y crea `PENDING_PAYMENT`; pago ⇒ `PAID`, `PaymentStatus.CONFIRMED`, `confirmSale` y factura; digital ⇒ `PAID → DELIVERED`; físico ⇒ `PAID → DISPATCHED` cuando todas las bodegas tienen envío en tránsito/entregado; `DISPATCHED → DELIVERED` con todos los envíos entregados; `DELIVERED → FINALIZED`; cancelación solo desde `PENDING_PAYMENT`. | `InvalidStateTransitionException` ante cualquier transición inválida. |
| D-09 | Matriz de responsabilidades **verificada** por extracción posicional del PDF: vendedores → ADMIN; productos → SELLER; inventario → SELLER + LOGISTIC_OPERATOR; pedidos → BUYER + SELLER + LOGISTIC_OPERATOR; reembolsos → BUYER + ADMIN. Las filas de devoluciones, reportes y Supervisor **no aparecen** en la matriz del PDF: devoluciones → BUYER solicita, ADMIN aprueba/rechaza, LOGISTIC_OPERATOR recibe; reportes → SUPERVISOR (solo lectura) son **[SUPUESTO]**. | Corrige la afirmación previa de “columnas perdidas”; ver `SDD/Spec-Ledger/passes/pass-4.md`. Las responsabilidades no verificadas no se implementan como regla obligatoria. |
| D-10 | Seis puertos de entrada: `PublicAccessPort`, `NaturalCustomerPort` (BUYER), `BusinessCustomerPort` (SELLER), `BusinessOperatorPort` (LOGISTIC_OPERATOR), `BusinessSupervisorPort` (SUPERVISOR), `TellerEmployeePort` (ADMIN). | `CommercialEmployeePort` e `InternalAnalystPort` **no se crean** (plantilla bancaria); se documenta la exclusión. |
| D-11 | JWT con `sub`, `jti`, `ver`, `iat`, `exp` y `role` informativo; sin PII. El filtro recarga el `User` vigente por `sub`. | `User.authTokenVersion` se incrementa en bloqueo/activación/baja; token obsoleto ⇒ 401. |
| D-12 | Precios con IVA incluido; tasa por defecto `0.19` configurable (`nexusmarket.tax.rate`) vía `TaxPolicyPort`. Factura: `total = order.total`, `subtotal = total/(1+rate)`, `taxes = total − subtotal`. Una factura por pedido pagado. | `InvoiceService.generateInvoice` usa `TaxPolicyPort`; redondeo `HALF_UP` escala 2. |
| D-13 | Producto nace `SUSPENDED`; `publish()` exige no descontinuado y, si es físico, ≥1 bodega y ≥1 registro de inventario; `DISCONTINUED` es terminal. | La verificación de inventario se hace en `CatalogService` con `InventoryRepositoryPort.existsByProductId`. |
| D-14 | Auditoría MongoDB `AuditLog(id, action, entityType, entityId, performedBy, role, occurredAt, details)` en alta de vendedor, bloqueo/activación de usuario, transiciones de pedido, movimientos de inventario, devoluciones, reembolsos y, por D-18, mutaciones de carrito. | `AuditPort.record(...)`; `AuditAction` y `AuditEntityType` acotados. |
| D-15 | Reembolso: monto > 0 y ≤ total del pedido; solo sobre devolución `APPROVED`/`COMPLETED`; suma de reembolsos de una devolución ≤ total del pedido. | `RefundService.generateRefund` valida el acumulado con `RefundRepositoryPort.findByReturnId`. |
| D-16 | Administrador inicial vía `CommandLineRunner` si no existe ningún `ADMIN`, usando `NEXUS_BOOTSTRAP_ADMIN_EMAIL` / `NEXUS_BOOTSTRAP_ADMIN_PASSWORD`. | Si faltan variables, no crea nada y registra un aviso. |
| D-17 | MySQL `nexusmarket_db` (usuario `nexus_app`) con JPA; MongoDB 4.4 `nexus_audit_db` (colección `audit_logs`); perfiles `local` y `docker`. | Los tests unitarios no dependen de bases de datos. MongoDB se fija en 4.4 porque el host de verificación no soporta AVX; las imágenes 5.0+ terminan con exit code 132. |
| D-18 | **Requisito docente**, no requisito del PDF: flujo demostrable de tres vendedores con devolución parcial por producto/cantidad y reporte de auditoría. | `Return` gana `productId`/`quantity` nullable; el overload de `ReturnService` reingresa solo el producto elegido y mantiene la devolución total legacy. `CartService` audita `CART_*` y `scripts/demo-flujo.ps1` ejecuta el flujo completo. |

## Supuestos nuevos (A-01…A-18)

| ID | Supuesto | Motivo |
|---|---|---|
| A-01 | El entorno de desarrollo usa **JDK 17 (Temurin 17.0.20.1)**. | El `pom.xml` declara `java.version=17`; el JDK se instaló en la Fase 0 con autorización. |
| A-02 | Se añade el catálogo `AuditEntityType` (`USER`, `BUYER`, `SELLER`, `WAREHOUSE`, `PRODUCT`, `INVENTORY`, `INVENTORY_MOVEMENT`, `CART`, `ORDER`, `INVOICE`, `SHIPMENT`, `RETURN`, `REFUND`). | D-14 exige `entityType`; R-04 prohíbe `String` abiertos en campos acotados. |
| A-03 | Se añade el modelo `AuthResult(String token, User user)` en `domain.models`. | `PublicAccessPort.login` debe devolver token + usuario; el dominio no depende de JWT. |
| A-04 | Se añade `InvalidProductStateException` en `domain.exceptions.product`. | Transiciones de producto inválidas (p. ej. publicar un descontinuado) no tienen excepción en la lista original. |
| A-05 | `Seller.warehouses` es una lista de `Warehouse` y `Warehouse.owner` referencia al `Seller`; la relación es bidireccional y se mapea en JPA con `@OneToMany(mappedBy="owner")` / `@ManyToOne`. | Mantiene los atributos exigidos por la rúbrica; los mappers evitan recursión al serializar. |
| A-06 | Si un usuario que no es `ADMIN` intenta registrar un vendedor, se lanza `SelfRegistrationNotAllowedException` (no `InsufficientPermissionException`). | Hace explícita la prohibición de autorregistro (R-08). |
| A-07 | `PaymentStatus.REFUNDED` queda reservado; procesar un reembolso **no** cambia por sí solo el estado de pago del pedido. | D-15 solo regula el reembolso; no se inventa una transición de pedido no especificada. |
| A-08 | `OrderService.finalize` lo ejecuta el `BUYER` dueño del pedido; el `ADMIN` también puede finalizarlo por soporte. | La spec no asigna la operación; se elige la opción menos privilegiada. |
| A-09 | `LogisticsService` recalcula automáticamente `OrderStatus.DISPATCHED` y `DELIVERED` al iniciar/entregar envíos; los controladores logísticos no exponen transiciones directas de pedido. | D-08 exige que el cambio ocurra cuando *todos* los envíos cumplen la condición. |
| A-10 | El número de factura se genera como `INV-<orderId>` (determinista y único por pedido). | Evita depender de secuencias externas y conserva la unicidad `invoices.order_id`. |
| A-11 | `Money` usa `java.util.Currency` con `COP` por defecto, escala 2 y `RoundingMode.HALF_UP`; no admite importes negativos. | R-05; los restos se validan por operación (precio > 0, reembolso > 0). |
| A-12 | Las devoluciones solo se solicitan sobre pedidos `DELIVERED` o `FINALIZED`; un pedido finalizado puede recibir una devolución porque esta no modifica sus datos. | D-08 + A.9; se documenta la excepción funcional a R-07. |
| A-13 | `OrderStatus.CART` no lo usa el agregado `Order` (el carrito es `Cart`); se conserva por trazabilidad. | D-03. |
| A-14 | `InventoryMovement.quantity` es positivo para `INFLOW`, `RESERVATION`, `SALE` y `RETURN`; para `ADJUSTMENT` puede ser negativo (nunca cero). | D-07 y `adjust(delta)`. |
| A-15 | Los reportes son modelos inmutables en `domain.models.reports` (`SalesReport`, `OrderSummary`, `InventorySummary`, `ReturnSummary`, `UserSummary`). | OBJ-12 y D-01; no se exponen entidades crudas en reportes. |
| A-16 | Los mensajes de excepción se escriben en inglés; los nombres de tests también. | §1 del prompt maestro y criterio 4 de la rúbrica. |
| A-17 | La liberación de una reserva al cancelar se registra como movimiento `RESERVATION` positivo. | El PDF solo define cinco movimientos y no incluye uno de liberación; se conserva el catálogo exigido y se documenta el mapeo. |
| A-18 | La auditoría MongoDB está fuera de la transacción JPA: no comparte transacción con MySQL. Si `AuditPort.record` falla, la excepción se propaga y revierte el trabajo de negocio ya ejecutado en la transacción JPA. | D-14 exige auditar; todavía no se usa outbox ni transacción distribuida. Se documenta el comportamiento actual y se revisará si la auditoría debe ser tolerante a fallos. |

## Ambigüedades resueltas por precedencia (§2)

| Ambigüedad | Resolución | Fuente que gana |
|---|---|---|
| `Shipped` vs `DISPATCHED`/`Despachado` | `DISPATCHED` en código; glosario §A.11 | Fuente 2 (prompt) |
| `CommercialStatus` `ACTIVE` vs `Eligible` | `ELIGIBLE`, `RESTRICTED`, `SUSPENDED` | Fuente 3 (rúbrica) |
| `MovementType` vs `InventoryMovementType` | `MovementType` | Fuente 2 (D-06) |
| 8 puertos de entrada vs 5 roles + público | 6 puertos (D-10) | Fuente 2 (D-10) |
| JWT con PII vs sin PII | Sin PII; solo `sub`, `jti`, `ver`, `iat`, `exp`, `role` informativo | Fuente 2 (D-11) |
