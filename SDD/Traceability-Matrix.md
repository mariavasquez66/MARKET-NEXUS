# Matriz de trazabilidad — NexusMarket

> Estado: **diseño completo (Fase 1)**; fases 2–5 y 6 implementadas. Las fases
> 7–8 están pendientes. La columna «Estado» conserva la foto del diseño y la
> evidencia de implementación se centraliza en `PROGRESS.md` y en las pruebas
> de cada fase.
>
> Fuentes: `SDD/especificacion_funcional_nexusmarket.md`,
> `SDD/Decisiones-y-supuestos.md`, `SDD/Domain/**`, `SDD/Adapters/**`.
> Toda fila debe poder rastrearse en ambos sentidos: requisito → artefacto y
> artefacto → requisito.

## 1. Objetivos → artefactos

| OBJ | Requisito | Entidades / VOs | Servicios | Puertos de entrada | Endpoints | Documento |
|---|---|---|---|---|---|---|
| OBJ-01 | Administración de usuarios | `User`, `Email`, `FullName`, `IdentityDocument`, `PasswordHash` | `AuthService`, `UserService` | `PublicAccessPort`, `TellerEmployeePort` | `/public/auth/login`, `/admin/users/**` | `Domain/services/AuthService.md`, `UserService.md` |
| OBJ-02 | Gestión de vendedores | `Seller`, `TaxId`, `Warehouse` | `SellerService`, `WarehouseService` | `TellerEmployeePort` | `/admin/sellers/**` | `SellerService.md`, `WarehouseService.md` |
| OBJ-03 | Gestión de compradores | `Buyer`, `Address`, `CommercialStatus` | `BuyerService` | `NaturalCustomerPort`, `PublicAccessPort` | `/public/buyers/register`, `/buyer/addresses/**` | `BuyerService.md` |
| OBJ-04 | Gestión de bodegas | `Warehouse`, `WarehouseType`, `Address` | `WarehouseService` | `TellerEmployeePort` | `/admin/warehouses/**`, `/admin/sellers/{id}/warehouses/**` | `WarehouseService.md` |
| OBJ-05 | Gestión de catálogo | `Product`, `ProductVariant`, `ProductType`, `ProductStatus`, `Money` | `CatalogService` | `BusinessCustomerPort`, `PublicAccessPort` | `/seller/products/**`, `/public/products/**` | `CatalogService.md` |
| OBJ-06 | Gestión de inventario | `Inventory`, `InventoryMovement`, `MovementType`, `InventoryCondition` | `InventoryService` | `BusinessOperatorPort`, `BusinessCustomerPort`, `NaturalCustomerPort` | `/logistics/inventory/**`, `/seller/inventory`, `/buyer/orders` | `InventoryService.md` |
| OBJ-07 | Carrito de compras | `Cart`, `CartItem`, `Money` | `CartService` | `NaturalCustomerPort` | `/buyer/cart/**` | `CartService.md` |
| OBJ-08 | Ciclo de pedidos | `Order`, `OrderStatus`, `PaymentStatus`, `StockAllocation`, `Address` | `OrderService` | `NaturalCustomerPort`, `BusinessCustomerPort`, `BusinessOperatorPort`, `BusinessSupervisorPort` | `/buyer/orders/**`, `/seller/orders`, `/logistics/orders`, `/supervisor/orders` | `OrderService.md` |
| OBJ-09 | Facturación | `Invoice`, `InvoiceStatus`, `Money` | `InvoiceService` | `NaturalCustomerPort` | `/buyer/orders/{id}/invoice` | `InvoiceService.md` |
| OBJ-10 | Logística y envíos | `Shipment`, `ShipmentStatus`, `Address` | `LogisticsService` | `BusinessOperatorPort`, `NaturalCustomerPort` | `/logistics/shipments/**` | `LogisticsService.md` |
| OBJ-11 | Devoluciones y reembolsos | `Return`, `Refund`, `ReturnStatus`, `RefundStatus`, `Money` | `ReturnService`, `RefundService` | `NaturalCustomerPort`, `BusinessOperatorPort`, `TellerEmployeePort` | `/buyer/orders/{id}/returns`, `/buyer/returns`, `/buyer/refunds`, `/logistics/returns/**`, `/admin/returns/**`, `/admin/refunds/**` | `ReturnService.md`, `RefundService.md` |
| OBJ-12 | Reportes y auditoría | `SalesReport`, `OrderSummary`, `InventorySummary`, `ReturnSummary`, `UserSummary`, `AuditLog` | `ReportService` | `BusinessSupervisorPort` | `/supervisor/reports/**`, `/supervisor/audit-logs` | `ReportService.md` |

## 2. Entidades y agregados

| Artefacto | Paquete | Agregado / raíz | Reglas | Documento | Estado |
|---|---|---|---|---|---|
| `User` | `domain.models` | raíz | RG-01, RG-02, R-09; `block/activate/deactivate`, `invalidateTokens` | `Domain-Model.md §2` | Diseñado |
| `Buyer` | `domain.models` | raíz | rol BUYER; direcciones; `CommercialStatus`; `assertCanPurchase` | `§3` | Diseñado |
| `Seller` | `domain.models` | raíz | R-08; `registerBy(admin,...)`; `assignWarehouse` | `§4` | Diseñado |
| `Warehouse` | `domain.models` | raíz | tipo/dueño; responsable; `WarehouseAlreadyAssigned` | `§5` | Diseñado |
| `Product` | `domain.models` | raíz | D-13; precio > 0; digital vs físico; variantes | `§6` | Diseñado |
| `Inventory` | `domain.models` | raíz | D-07; nunca negativo; `DAMAGED` no reserva/venta | `§7` | Diseñado |
| `InventoryMovement` | `domain.models` | entidad inmutable | trazabilidad de movimientos | `§7` | Diseñado |
| `Cart` | `domain.models` | raíz | cantidades > 0; sin duplicados; solo `PUBLISHED` | `§8` | Diseñado |
| `CartItem` | `domain.models` | parte de `Cart`/`Order` | producto, cantidad, precio unitario | `§8` | Diseñado |
| `Order` | `domain.models` | raíz | D-03/D-08; R-07; `stockAllocations`; `invoice` | `§8` | Diseñado |
| `Invoice` | `domain.models` | raíz | D-12; una por pedido; `voidInvoice` | `§8` | Diseñado |
| `Shipment` | `domain.models` | raíz | solo físicos; `PENDING→IN_TRANSIT→DELIVERED` | `§8` | Diseñado |
| `Return` | `domain.models` | raíz | solo pedidos entregados/finalizados; `approve/reject/complete` | `§8` | Diseñado |
| `Refund` | `domain.models` | raíz | D-15; `process()` una vez | `§8` | Diseñado |
| `AuditLog` | `domain.models` | inmutable | D-14; Mongo | `§9` | Diseñado |
| `AuthResult` | `domain.models` | resultado | `token` + `user` | `§10` | Diseñado |
| `SalesReport` | `domain.models.reports` | resultado | D-12/OBJ-12 | `§10` | Diseñado |
| `OrderSummary` | `domain.models.reports` | resultado | OBJ-12 | `§10` | Diseñado |
| `InventorySummary` | `domain.models.reports` | resultado | OBJ-12 | `§10` | Diseñado |
| `ReturnSummary` | `domain.models.reports` | resultado | OBJ-12 | `§10` | Diseñado |
| `UserSummary` | `domain.models.reports` | resultado | OBJ-12 | `§10` | Diseñado |

## 3. Catálogos (`domain.enums`)

| Catálogo | Valores | Documento | Estado |
|---|---|---|---|
| `DomainCatalog` | base abstracta (`getCode`, `fromCode`, `values`) | `Enums-and-Value-Objects.md §1` | Implementado (Fase 0, se amplía) |
| `UserRole` | `ADMIN, BUYER, SELLER, LOGISTIC_OPERATOR, SUPERVISOR` | `§2` | Implementado (Fase 0, revisar `values`) |
| `UserStatus` | `ACTIVE, BLOCKED, INACTIVE` | `§2` | Implementado (se amplía) |
| `CommercialStatus` | `ELIGIBLE, RESTRICTED, SUSPENDED` | `§2` (D-04) | Implementado (se renombra) |
| `ProductType` | `PHYSICAL, DIGITAL` | `§2` | Implementado |
| `ProductStatus` | `SUSPENDED, PUBLISHED, DISCONTINUED` | `§2` (D-13) | Implementado |
| `OrderStatus` | `CART, PENDING_PAYMENT, PAID, DISPATCHED, DELIVERED, FINALIZED, CANCELLED` | `§2` (D-03) | Implementado |
| `PaymentStatus` | `PENDING, CONFIRMED` | `§2` | Implementado |
| `ShipmentStatus` | `PENDING, IN_TRANSIT, DELIVERED` | `§2` | Implementado |
| `ReturnStatus` | `REQUESTED, APPROVED, REJECTED, COMPLETED` | `§2` | Implementado |
| `WarehouseType` | `MARKETPLACE, SELLER` | `§3` (D-06) | Diseñado |
| `MovementType` | `INFLOW, RESERVATION, SALE, ADJUSTMENT, RETURN` | `§3` | Diseñado |
| `InventoryCondition` | `GOOD, DAMAGED` | `§3` | Diseñado |
| `InvoiceStatus` | `ISSUED, VOIDED` | `§3` | Diseñado |
| `RefundStatus` | `PENDING, PROCESSED` | `§3` | Diseñado |
| `VariantType` | `COLOR, SIZE, MODEL, OTHER` | `§3` | Diseñado |
| `AuditAction` | altas, bloqueos, transiciones, inventario, devoluciones, reembolsos | `§3` (D-14) | Diseñado |
| `AuditEntityType` | `USER, SELLER, ORDER, INVENTORY, RETURN, REFUND` | `§3` | Diseñado |

## 4. Value Objects (`domain.valueobjects`)

| VO | Invariante | Usado por | Documento | Estado |
|---|---|---|---|---|
| `Money` | `BigDecimal` escala 2 `HALF_UP`; moneda `COP`; ≥ 0 | `Product`, `CartItem`, `Order`, `Invoice`, `Refund`, reportes | `Enums-and-Value-Objects.md §5` | Diseñado |
| `Address` | campos no vacíos; `postalCode` opcional | `Buyer`, `Warehouse`, `Order`, `Shipment` | `§5` | Diseñado |
| `Email` | formato válido; normalizado a minúsculas | `User` | `§5` | Diseñado |
| `FullName` | no vacío | `User` | `§5` | Diseñado |
| `IdentityDocument` | no vacío; formato simple | `User` | `§5` | Diseñado |
| `TaxId` | formato fiscal simple documentado | `Seller` | `§5` | Diseñado |
| `ProductVariant` | `type` + `value` no vacío | `Product` | `§5` | Diseñado |
| `PasswordHash` | opaco, no vacío | `User` | `§5` | Diseñado |
| `StockAllocation` | `productId`, `warehouseId`, `quantity > 0` | `Order` | `§5` | Diseñado |

## 5. Servicios de dominio

| Servicio | Operaciones principales | Puertos de salida | Excepciones propias | Tests | Documento |
|---|---|---|---|---|---|
| `AuthService` | `login`, `registerBuyer` | `UserRepositoryPort`, `BuyerRepositoryPort`, `PasswordServicePort`, `JwtServicePort`, `AuditPort`, `ClockPort` | `UnauthenticatedUserException`, `DuplicateEmailException`, `DuplicateIdentificationException`, `InactiveUserException` | `AuthServiceTest` | `services/AuthService.md` |
| `UserService` | crear/listar/obtener/actualizar, `block/activate/deactivate` | `UserRepositoryPort`, `AuditPort` | `UserNotFoundException`, `DuplicateEmailException`, `DuplicateIdentificationException`, `InsufficientPermissionException`, `SingleRoleException` | `UserServiceTest` | `services/UserService.md` |
| `BuyerService` | direcciones, `restrict/suspend/markEligible` | `BuyerRepositoryPort`, `AuditPort` | `BuyerNotFoundException`, `InvalidAddressException`, `UnauthorizedOperationException` | `BuyerServiceTest` | `services/BuyerService.md` |
| `SellerService` | `registerSeller`, `assignWarehouse`, `updateTaxId`, consultas | `SellerRepositoryPort`, `UserRepositoryPort`, `WarehouseRepositoryPort`, `AuditPort` | `SellerNotFoundException`, `SelfRegistrationNotAllowedException`, `DuplicateEntityException` | `SellerServiceTest` | `services/SellerService.md` |
| `WarehouseService` | crear/actualizar/listar | `WarehouseRepositoryPort`, `SellerRepositoryPort`, `UserRepositoryPort` | `WarehouseNotFoundException`, `InvalidWarehouseTypeException`, `WarehouseAlreadyAssignedException` | `WarehouseServiceTest` | `services/WarehouseService.md` |
| `CatalogService` | alta/edición/variantes/publicar/suspender/descontinuar | `ProductRepositoryPort`, `SellerRepositoryPort`, `WarehouseRepositoryPort`, `InventoryRepositoryPort`, `AuditPort` | `ProductNotFoundException`, `ProductNotPublishedException`, `InvalidPriceException`, `InvalidProductTypeException`, `InvalidProductStateException`, `SellerWithoutWarehouseException` | `CatalogServiceTest` | `services/CatalogService.md` |
| `InventoryService` | alta, `addStock`, `reserve`, `release`, `confirmSale`, `adjust`, `registerReturn`, `markDamaged/Repaired` | `InventoryRepositoryPort`, `InventoryMovementRepositoryPort`, `ProductRepositoryPort`, `AuditPort`, `ClockPort` | `InventoryNotFoundException`, `InsufficientStockException`, `NegativeStockException`, `DamagedInventoryException`, `InvalidReservationException` | `InventoryServiceTest` | `services/InventoryService.md` |
| `CartService` | `getCart`, `addItem`, `updateQuantity`, `removeItem`, `clear` | `CartRepositoryPort`, `ProductRepositoryPort`, `BuyerRepositoryPort` | `CartNotFoundException`, `EmptyCartException`, `ProductNotAvailableException`, `InvalidQuantityException`, `RestrictedBuyerException` | `CartServiceTest` | `services/CartService.md` |
| `OrderService` | `checkout`, `confirmPayment`, `cancel`, `markDispatched/Delivered`, `finalize`, consultas | `OrderRepositoryPort`, `CartRepositoryPort`, `InventoryRepositoryPort`, `BuyerRepositoryPort`, `AuditPort`, `ClockPort` | `OrderNotFoundException`, `InvalidStateTransitionException`, `FinalizedOrderException`, `CancelledOrderException`, `EmptyCartException`, `InsufficientStockException` | `OrderServiceTest` | `services/OrderService.md` |
| `InvoiceService` | `generateInvoice`, `get`, `listByBuyer`, `void` | `InvoiceRepositoryPort`, `OrderRepositoryPort`, `TaxPolicyPort`, `ClockPort` | `InvoiceNotFoundException`, `InvoiceAlreadyExistsException`, `VoidedInvoiceException` | `InvoiceServiceTest` | `services/InvoiceService.md` |
| `LogisticsService` | `createShipment`, `startShipping`, `confirmDelivery` | `ShipmentRepositoryPort`, `OrderRepositoryPort`, `WarehouseRepositoryPort`, `AuditPort`, `ClockPort` | `ShipmentNotFoundException`, `ShipmentAlreadyStartedException`, `InvalidDestinationAddressException`, `InvalidProductTypeException` | `LogisticsServiceTest` | `services/LogisticsService.md` |
| `ReturnService` | `requestReturn`, `approve`, `reject`, `receive/complete` | `ReturnRepositoryPort`, `OrderRepositoryPort`, `InventoryRepositoryPort`, `AuditPort`, `ClockPort` | `ReturnNotFoundException`, `ReturnNotAllowedException`, `ReturnAlreadyProcessedException` | `ReturnServiceTest` | `services/ReturnService.md` |
| `RefundService` | `generateRefund`, `process`, `listByReturn` | `RefundRepositoryPort`, `ReturnRepositoryPort`, `OrderRepositoryPort`, `AuditPort`, `ClockPort` | `RefundNotFoundException`, `InvalidRefundAmountException`, `RefundAlreadyProcessedException`, `ReturnNotAllowedException` | `RefundServiceTest` | `services/RefundService.md` |
| `ReportService` | `salesReport`, `orderSummary`, `inventorySummary`, `returnSummary`, `userSummary`, `auditLogs` | `OrderRepositoryPort`, `InventoryRepositoryPort`, `ReturnRepositoryPort`, `UserRepositoryPort`, `AuditPort` | `InvalidDateRangeException`, `NoDataException` | `ReportServiceTest` | `services/ReportService.md` |

## 6. Puertos de entrada

| Puerto | Rol | Métodos | Caso de uso | Controlador | Documento |
|---|---|---|---|---|---|
| `PublicAccessPort` | público | 4 | `PublicAccessUseCase` | `PublicController` | `Domain/Input-ports.md §1` |
| `NaturalCustomerPort` | `BUYER` | 17 | `NaturalCustomerUseCase` | `BuyerController` | `§2` |
| `BusinessCustomerPort` | `SELLER` | 10 | `BusinessCustomerUseCase` | `SellerController` | `§3` |
| `BusinessOperatorPort` | `LOGISTIC_OPERATOR` | 8 | `BusinessOperatorUseCase` | `LogisticsController` | `§4` |
| `BusinessSupervisorPort` | `SUPERVISOR` | 7 | `BusinessSupervisorUseCase` | `SupervisorController` | `§5` |
| `TellerEmployeePort` | `ADMIN` | 17 | `TellerEmployeeUseCase` | `AdminController` | `§6` |

`CommercialEmployeePort` e `InternalAnalystPort` **no se crean** (D-10).

## 7. Puertos de salida → adaptadores

| Puerto | Implementación JPA/Mongo/Security | Documento |
|---|---|---|
| `UserRepositoryPort` | `UserJpaAdapter` | `Domain/Output-ports.md §1` |
| `BuyerRepositoryPort` | `BuyerJpaAdapter` | `§1` |
| `SellerRepositoryPort` | `SellerJpaAdapter` | `§1` |
| `WarehouseRepositoryPort` | `WarehouseJpaAdapter` | `§1` |
| `ProductRepositoryPort` | `ProductJpaAdapter` | `§1` |
| `InventoryRepositoryPort` | `InventoryJpaAdapter` | `§1` |
| `InventoryMovementRepositoryPort` | `InventoryMovementJpaAdapter` | `§1` |
| `CartRepositoryPort` | `CartJpaAdapter` | `§1` |
| `OrderRepositoryPort` | `OrderJpaAdapter` | `§1` |
| `InvoiceRepositoryPort` | `InvoiceJpaAdapter` | `§1` |
| `ShipmentRepositoryPort` | `ShipmentJpaAdapter` | `§1` |
| `ReturnRepositoryPort` | `ReturnJpaAdapter` | `§1` |
| `RefundRepositoryPort` | `RefundJpaAdapter` | `§1` |
| `AuditPort` | `AuditLogMongoAdapter` | `§2` |
| `PasswordServicePort` | `PasswordServiceAdapter` (BCrypt) | `§2` |
| `JwtServicePort` | `JwtProvider` | `§2` |
| `TaxPolicyPort` | `TaxPolicyAdapter` | `§2` |
| `ClockPort` | `SystemClockAdapter` | `§2` |

## 8. Excepciones → HTTP

| Paquete | Excepciones | HTTP |
|---|---|---|
| `common` | `InvalidDataException`, `DuplicateEntityException`, `UnauthenticatedUserException`, `SingleRoleException`, `InsufficientPermissionException`, `UnauthorizedOperationException`, `InvalidDateRangeException`, `NoDataException` | 400/401/403/404/409 según `Exceptions.md §4` |
| `user` | `UserNotFoundException`, `DuplicateEmailException`, `DuplicateIdentificationException`, `InactiveUserException`, `InvalidRoleException` | 404/409/403/403/403 |
| `buyer` | `BuyerNotFoundException`, `RestrictedBuyerException`, `InvalidAddressException` | 404/403/400 |
| `seller` | `SellerNotFoundException`, `SelfRegistrationNotAllowedException`, `SellerWithoutWarehouseException` | 404/403/409 |
| `warehouse` | `WarehouseNotFoundException`, `InvalidWarehouseTypeException`, `WarehouseAlreadyAssignedException` | 404/400/409 |
| `product` | `ProductNotFoundException`, `ProductNotPublishedException`, `InvalidPriceException`, `InvalidProductTypeException`, `InvalidProductStateException` | 404/404/400/400/409 |
| `inventory` | `InventoryNotFoundException`, `InsufficientStockException`, `NegativeStockException`, `DamagedInventoryException`, `InvalidReservationException` | 404/409/409/409/409 |
| `cart` | `CartNotFoundException`, `EmptyCartException`, `ProductNotAvailableException`, `InvalidQuantityException` | 404/400/409/400 |
| `order` | `OrderNotFoundException`, `InvalidStateTransitionException`, `FinalizedOrderException`, `CancelledOrderException` | 404/409/409/409 |
| `invoice` | `InvoiceNotFoundException`, `InvoiceAlreadyExistsException`, `VoidedInvoiceException` | 404/409/409 |
| `logistics` | `ShipmentNotFoundException`, `ShipmentAlreadyStartedException`, `InvalidDestinationAddressException` | 404/409/400 |
| `returns` | `ReturnNotFoundException`, `ReturnNotAllowedException`, `ReturnAlreadyProcessedException` | 404/409/409 |
| `refund` | `RefundNotFoundException`, `InvalidRefundAmountException`, `RefundAlreadyProcessedException` | 404/409/409 |

Todas heredan de `DomainException`; mensajes en inglés; sin datos sensibles.

## 9. Endpoints → puerto → rol

| Prefijo | Puerto de entrada | Rol | Nº endpoints |
|---|---|---|---|
| `/api/v1/public/**` | `PublicAccessPort` | público | 4 |
| `/api/v1/buyer/**` | `NaturalCustomerPort` | `BUYER` | 16 |
| `/api/v1/seller/**` | `BusinessCustomerPort` | `SELLER` | 10 |
| `/api/v1/logistics/**` | `BusinessOperatorPort` | `LOGISTIC_OPERATOR` | 10 |
| `/api/v1/admin/**` | `TellerEmployeePort` | `ADMIN` | 17 |
| `/api/v1/supervisor/**` | `BusinessSupervisorPort` | `SUPERVISOR` | 7 |

Detalle en `SDD/Adapters/Api-rest-endpoints.md`.

## 10. Requisitos transversales → evidencia prevista

| Regla | Evidencia en código (Fase 2/3) | Test previsto |
|---|---|---|
| RG-01 autenticación | `User.assertCanOperate`, filtro JWT, `UnauthenticatedUserException` | `JwtAuthenticationFilterTest`, `AuthServiceTest` |
| RG-02 rol único | `User` sin `changeRole`; `SingleRoleException` | `UserTest`, `UserServiceTest` |
| RG-03 autorización | `InsufficientPermissionException`, `UnauthorizedOperationException` | `*ServiceTest` por rol |
| R-03 entidades ricas | constructores/factorías y métodos de intención | tests por agregado |
| R-04 sin Strings acotados | catálogos + `fromCode` en el borde | `*RestMapperTest`, converters |
| R-05 dinero | `Money` + `BigDecimal` | `MoneyTest` |
| R-06 stock | `Inventory.reserve/confirmSale/adjust` | `InventoryTest`, `InventoryServiceTest` |
| R-07 pedido no modificable | `Order.assertModifiable` | `OrderTest`, `OrderServiceTest` |
| R-08 vendedor solo ADMIN | `Seller.registerBy` | `SellerTest`, `SellerServiceTest` |
| R-09 unicidad | índices únicos + servicios | `UserServiceTest`, `SellerServiceTest` |
| D-07 inventario | métodos y movimientos | `InventoryTest` |
| D-08 flujo de pedido | máquina de estados | `OrderTest`, escenarios de `Domain-Services.md §4` |
| D-11 JWT | claims `sub/jti/ver/iat/exp/role` | `JwtProviderTest`, `JwtAuthenticationFilterTest` |
| D-12 impuestos | `TaxPolicyPort` + `InvoiceService` | `InvoiceServiceTest` |
| D-13 publicación | `Product.publish` | `CatalogServiceTest` |
| D-14 auditoría | `AuditPort.record` en servicios | tests con `AuditPort` mockeado |
| D-15 reembolso | `Refund` + `RefundService` | `RefundTest`, `RefundServiceTest` |
| D-16 admin inicial | `BootstrapAdminRunner` + `UserService.createBootstrapAdmin` | `BootstrapAdminRunnerTest`, `UserServiceBootstrapTest` |
| Seguridad REST | `SecurityConfig`, `JwtAuthenticationFilter`, `SecurityErrorWriter` | `SecurityConfigTest`, `JwtAuthenticationFilterTest`, `JwtProviderTest` |
| CORS / `X-Request-Id` | `SecurityConfig.corsConfigurationSource`, `RequestIdFilter` | `SecurityConfigTest` |

## 11. Verificación de exhaustividad

- [x] 18 catálogos incluyendo `DomainCatalog` (17 + base).
- [x] 9 Value Objects.
- [x] 21 modelos: 14 agregados/entidades + `CartItem` + 6 modelos de resultado
  (`AuthResult`, `SalesReport`, `OrderSummary`, `InventorySummary`,
  `ReturnSummary`, `UserSummary`).
- [x] 14 servicios de dominio con documento propio.
- [x] 6 puertos de entrada (sin los 2 bancarios descartados).
- [x] 18 puertos de salida.
- [x] 52 excepciones concretas + `DomainException` abstracta.
- [x] 6 prefijos REST con 64 endpoints.
- [x] Reglas R-01…R-12 y RG-01…RG-03 con evidencia/test previsto.

## 12. Riesgos y pendientes de Fase 1

| ID | Riesgo | Mitigación |
|---|---|---|
| D-09 | Matriz de responsabilidades no confirmada contra el PDF original | Marcada ⚠ en `Decisiones-y-supuestos.md`; validar con la persona dueña antes de Fase 5 |
| A-16 | `AuthResult` como modelo de dominio (no VO) | Se mantiene en `domain.models`; el mapper REST lo convierte a `AuthResponse` |
| — | `Return` es palabra reservada contextual en Java | El modelo se llama `Return`; los mapeadores/adaptadores lo usan con nombre completo cuando haga falta |
| — | Enums actuales sin `values()`/BOM | Se corrigen en Fase 2 antes de usarlos |

