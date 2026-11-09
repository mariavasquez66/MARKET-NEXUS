# Servicios de dominio — índice y reglas transversales

> Paquete: `application.domain.services`. Son POJOs puros (sin anotaciones de
> Spring), se instancian con `@Configuration` + `@Bean` en
> `application.infrastructure.config.DomainServicesConfig` y reciben sus
> dependencias por constructor. No acceden a HTTP, JPA, Mongo ni Jackson.

## 1. Índice de servicios

| Servicio | OBJ | Dependencias (puertos de salida) | Documento |
|---|---|---|---|
| `AuthService` | OBJ-01/03 | `UserRepositoryPort`, `BuyerRepositoryPort`, `PasswordServicePort`, `JwtServicePort`, `AuditPort`, `ClockPort` | `services/AuthService.md` |
| `UserService` | OBJ-01 | `UserRepositoryPort`, `PasswordServicePort`, `AuditPort`, `ClockPort` | `services/UserService.md` |
| `BuyerService` | OBJ-03 | `BuyerRepositoryPort`, `AuditPort`, `ClockPort` | `services/BuyerService.md` |
| `BuyerEligibilityService` | OBJ-03 | `BuyerRepositoryPort` | `services/BuyerEligibilityService.md` |
| `SellerService` | OBJ-02 | `SellerRepositoryPort`, `UserRepositoryPort`, `WarehouseRepositoryPort`, `AuditPort`, `ClockPort` | `services/SellerService.md` |
| `WarehouseService` | OBJ-04 | `WarehouseRepositoryPort`, `SellerRepositoryPort`, `UserRepositoryPort`, `AuditPort`, `ClockPort` | `services/WarehouseService.md` |
| `CatalogService` | OBJ-05 | `ProductRepositoryPort`, `SellerRepositoryPort`, `InventoryRepositoryPort`, `AuditPort`, `ClockPort` | `services/CatalogService.md` |
| `CatalogQueryService` | OBJ-05 | `ProductRepositoryPort`, `SellerRepositoryPort` | `services/CatalogQueryService.md` |
| `InventoryService` | OBJ-06 | `InventoryRepositoryPort`, `InventoryMovementRepositoryPort`, `ProductRepositoryPort`, `WarehouseRepositoryPort`, `AuditPort`, `ClockPort` | `services/InventoryService.md` |
| `InventoryQueryService` | OBJ-06 | `InventoryRepositoryPort` | `services/InventoryQueryService.md` |
| `CartService` | OBJ-07 | `CartRepositoryPort`, `BuyerRepositoryPort`, `ProductRepositoryPort` | `services/CartService.md` |
| `OrderService` | OBJ-08 | Fachada de los cinco servicios de pedido | `services/OrderService.md` |
| `CheckoutService` | OBJ-08 | `CartRepositoryPort`, `BuyerRepositoryPort`, `OrderRepositoryPort`, `ProductRepositoryPort`, `InventoryRepositoryPort`, `InventoryMovementRepositoryPort`, `AuditPort`, `ClockPort` | `services/CheckoutService.md` |
| `OrderPaymentService` | OBJ-08 | `OrderRepositoryPort`, `InventoryRepositoryPort`, `InventoryMovementRepositoryPort`, `InvoiceService`, `AuditPort`, `ClockPort` | `services/OrderPaymentService.md` |
| `OrderFulfillmentService` | OBJ-08 | `OrderRepositoryPort`, `ShipmentRepositoryPort`, `AuditPort`, `ClockPort` | `services/OrderFulfillmentService.md` |
| `OrderLifecycleService` | OBJ-08 | `OrderRepositoryPort`, `InventoryRepositoryPort`, `InventoryMovementRepositoryPort`, `AuditPort`, `ClockPort` | `services/OrderLifecycleService.md` |
| `OrderQueryService` | OBJ-08 | `BuyerRepositoryPort`, `OrderRepositoryPort` | `services/OrderQueryService.md` |
| `InvoiceService` | OBJ-09 | `InvoiceRepositoryPort`, `OrderRepositoryPort`, `TaxPolicyPort`, `AuditPort`, `ClockPort` | `services/InvoiceService.md` |
| `LogisticsService` | OBJ-10 | `ShipmentRepositoryPort`, `OrderRepositoryPort`, `WarehouseRepositoryPort`, `OrderService`, `AuditPort`, `ClockPort` | `services/LogisticsService.md` |
| `ReturnService` | OBJ-11 | `ReturnRepositoryPort`, `OrderRepositoryPort`, `InventoryService`, `AuditPort`, `ClockPort` | `services/ReturnService.md` |
| `RefundService` | OBJ-11 | `RefundRepositoryPort`, `ReturnRepositoryPort`, `OrderRepositoryPort`, `AuditPort`, `ClockPort` | `services/RefundService.md` |
| `ReportService` | OBJ-12 | `OrderRepositoryPort`, `InventoryRepositoryPort`, `ReturnRepositoryPort`, `UserRepositoryPort`, `AuditPort`, `ClockPort` | `services/ReportService.md` |

## 2. Reglas transversales obligatorias

1. **Actor autenticado (RG-01).** Todos los métodos reciben `User actor` como
   primer parámetro, salvo `AuthService.login` y `AuthService.registerBuyer`
   (públicos). El primer paso es `actor.assertCanOperate()`; si el usuario no
   está `ACTIVE` ⇒ `InactiveUserException`.
2. **Rol único y autorización (RG-02/RG-03).** Cada método verifica
   `actor.hasRole(...)`; si no ⇒ `InsufficientPermissionException` o
   `UnauthorizedOperationException` según el caso. El rol no se deduce del
   cliente: llega desde el `User` recargado por el filtro JWT.
3. **Propiedad de los datos.** Un `BUYER` solo opera sobre su propio `Buyer`,
   carrito, pedidos, devoluciones y reembolsos; un `SELLER` solo sobre sus
   productos, inventarios y pedidos que contienen sus productos. Violación ⇒
   `UnauthorizedOperationException`.
4. **Validación antes de mutar.** Los servicios construyen los Value Objects,
   validan unicidad y estado, y solo después llaman a los métodos de negocio de
   las entidades. Nunca cambian estado con setters.
5. **Auditoría (D-14).** Tras una mutación exitosa se llama a
   `AuditPort.record(AuditLog.create(...))`. Si la operación falla, no se
   registra auditoría de éxito. Las acciones y entidades auditables están en
   `AuditAction`/`AuditEntityType`.
6. **Tiempo.** Ningún servicio usa `Instant.now()`; se inyecta `ClockPort` y se
   pasa el instante a las factorías.
7. **Persistencia.** Los servicios solo conocen puertos de salida y devuelven
   tipos de dominio. La transacción se delimita en el adaptador de caso de uso
   (`@Transactional` en infraestructura), no en el dominio.
8. **Mensajes.** Todas las excepciones llevan mensaje en inglés; los documentos
   SDD y los comentarios de negocio se mantienen en español.
9. **Excepciones.** Se lanzan las excepciones específicas del subpaquete
   correspondiente; nunca `IllegalArgumentException` para reglas de negocio.
10. **Consultas.** Las operaciones de lectura también validan actor y rol; los
    reportes son de solo lectura para `SUPERVISOR`.

## 3. Matriz de auditoría obligatoria

| Evento | `AuditAction` | `AuditEntityType` | Servicio |
|---|---|---|---|
| Alta de usuario interno | `USER_CREATED` | `USER` | `UserService` |
| Registro público de comprador | `USER_CREATED` | `USER` | `AuthService` |
| Bloqueo / activación / baja de usuario | `USER_BLOCKED` / `USER_ACTIVATED` / `USER_DEACTIVATED` | `USER` | `UserService` |
| Alta de vendedor | `SELLER_REGISTERED` | `SELLER` | `SellerService` |
| Actualización de `TaxId` | `SELLER_TAX_ID_UPDATED` | `SELLER` | `SellerService` |
| Creación / actualización de bodega | `WAREHOUSE_CREATED` / `WAREHOUSE_UPDATED` | `WAREHOUSE` | `WarehouseService` |
| Asignación de bodega a vendedor | `WAREHOUSE_ASSIGNED` | `WAREHOUSE` | `SellerService` |
| Registro / actualización / publicación / suspensión / descontinuación de producto | `PRODUCT_REGISTERED`, `PRODUCT_UPDATED`, `PRODUCT_PUBLISHED`, `PRODUCT_SUSPENDED`, `PRODUCT_DISCONTINUED` | `PRODUCT` | `CatalogService` |
| Movimiento de inventario | `INVENTORY_MOVEMENT` | `INVENTORY` | `InventoryService` |
| Creación de pedido | `ORDER_CREATED` | `ORDER` | `OrderService` |
| Transición de pedido | `ORDER_STATUS_CHANGED` | `ORDER` | `OrderService` |
| Emisión / anulación de factura | `INVOICE_ISSUED` / `INVOICE_VOIDED` | `INVOICE` | `InvoiceService` |
| Envío creado / iniciado / entregado | `SHIPMENT_CREATED` / `SHIPMENT_STARTED` / `SHIPMENT_DELIVERED` | `SHIPMENT` | `LogisticsService` |
| Devolución solicitada / aprobada / rechazada / completada | `RETURN_REQUESTED` / `RETURN_APPROVED` / `RETURN_REJECTED` / `RETURN_COMPLETED` | `RETURN` | `ReturnService` |
| Reembolso generado / procesado | `REFUND_CREATED` / `REFUND_PROCESSED` | `REFUND` | `RefundService` |

`details` describe la operación en inglés, por ejemplo
`"Order status changed from PAID to DISPATCHED"`.

## 4. Escenarios de extremo a extremo a cubrir en Fase 3

1. Compra física completa: checkout → pago → envío → entrega → finalización →
   factura.
2. Compra digital: checkout → pago → `DELIVERED` automático → finalización →
   factura.
3. Cancelación desde `PENDING_PAYMENT` con liberación de reservas.
4. Devolución aprobada y reembolso procesado.
5. Intento de sobreventa (`InsufficientStockException`).
6. Intento de reservar inventario `DAMAGED` (`DamagedInventoryException`).
7. Intento de modificar un pedido `FINALIZED` (`FinalizedOrderException`).
8. Registro de vendedor por un no administrador
   (`SelfRegistrationNotAllowedException`).
9. Correo o documento duplicado (`DuplicateEmailException` /
   `DuplicateIdentificationException`).
10. Token obsoleto o usuario bloqueado → 401 (Fase 6).
