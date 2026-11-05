# Matriz de trazabilidad — NexusMarket

> Estado: **implementación completa (Fases 0–8)**. La columna «Estado» refleja
> los artefactos implementados; la evidencia de ejecución se centraliza en
> `PROGRESS.md` y en las pruebas de cada fase.
>
> Rutas base de los artefactos: `src/main/java/application/domain/`,
> `src/main/java/application/adapters/` y
> `src/main/java/application/infrastructure/`. Las pruebas están en
> `src/test/java/application/`.
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
| `User` | `domain.models` | raíz | RG-01, RG-02, R-09; `block/activate/deactivate`, `invalidateTokens` | `Domain-Model.md §2` | Implementado |
| `Buyer` | `domain.models` | raíz | rol BUYER; direcciones; `CommercialStatus`; `assertCanPurchase` | `§3` | Implementado |
| `Seller` | `domain.models` | raíz | R-08; `registerBy(admin,...)`; `assignWarehouse` | `§4` | Implementado |
| `Warehouse` | `domain.models` | raíz | tipo/dueño; responsable; `WarehouseAlreadyAssignedException` | `§5` | Implementado |
| `Product` | `domain.models` | raíz | D-13; precio > 0; digital vs físico; variantes | `§6` | Implementado |
| `Inventory` | `domain.models` | raíz | D-07; nunca negativo; `DAMAGED` no reserva/venta | `§7` | Implementado |
| `InventoryMovement` | `domain.models` | entidad inmutable | trazabilidad de movimientos | `§7` | Implementado |
| `Cart` | `domain.models` | raíz | cantidades > 0; sin duplicados; solo `PUBLISHED` | `§8` | Implementado |
| `CartItem` | `domain.models` | parte de `Cart`/`Order` | producto, cantidad, precio unitario | `§8` | Implementado |
| `Order` | `domain.models` | raíz | D-03/D-08; R-07; `stockAllocations`; `invoice` | `§8` | Implementado |
| `Invoice` | `domain.models` | raíz | D-12; una por pedido; `voidInvoice` | `§8` | Implementado |
| `Shipment` | `domain.models` | raíz | solo físicos; `PENDING→IN_TRANSIT→DELIVERED` | `§8` | Implementado |
| `Return` | `domain.models` | raíz | solo pedidos entregados/finalizados; `approve/reject/complete` | `§8` | Implementado |
| `Refund` | `domain.models` | raíz | D-15; `process()` una vez | `§8` | Implementado |
| `AuditLog` | `domain.models` | inmutable | D-14; Mongo | `§9` | Implementado |
| `AuthResult` | `domain.models` | resultado | `token` + `user` | `§10` | Implementado |
| `SalesReport` | `domain.models.reports` | resultado | D-12/OBJ-12 | `§10` | Implementado |
| `OrderSummary` | `domain.models.reports` | resultado | OBJ-12 | `§10` | Implementado |
| `InventorySummary` | `domain.models.reports` | resultado | OBJ-12 | `§10` | Implementado |
| `ReturnSummary` | `domain.models.reports` | resultado | OBJ-12 | `§10` | Implementado |
| `UserSummary` | `domain.models.reports` | resultado | OBJ-12 | `§10` | Implementado |

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
| `WarehouseType` | `MARKETPLACE, SELLER` | `§3` (D-06) | Implementado |
| `MovementType` | `INFLOW, RESERVATION, SALE, ADJUSTMENT, RETURN` | `§3` | Implementado |
| `InventoryCondition` | `GOOD, DAMAGED` | `§3` | Implementado |
| `InvoiceStatus` | `ISSUED, VOIDED` | `§3` | Implementado |
| `RefundStatus` | `PENDING, PROCESSED` | `§3` | Implementado |
| `VariantType` | `COLOR, SIZE, MODEL, OTHER` | `§3` | Implementado |
| `AuditAction` | altas, bloqueos, transiciones, inventario, devoluciones, reembolsos | `§3` (D-14) | Implementado |
| `AuditEntityType` | `USER, SELLER, ORDER, INVENTORY, RETURN, REFUND` | `§3` | Implementado |

## 4. Value Objects (`domain.valueobjects`)

| VO | Invariante | Usado por | Documento | Estado |
|---|---|---|---|---|
| `Money` | `BigDecimal` escala 2 `HALF_UP`; moneda `COP`; ≥ 0 | `Product`, `CartItem`, `Order`, `Invoice`, `Refund`, reportes | `Enums-and-Value-Objects.md §5` | Implementado |
| `Address` | campos no vacíos; `postalCode` opcional | `Buyer`, `Warehouse`, `Order`, `Shipment` | `§5` | Implementado |
| `Email` | formato válido; normalizado a minúsculas | `User` | `§5` | Implementado |
| `FullName` | no vacío | `User` | `§5` | Implementado |
| `IdentityDocument` | no vacío; formato simple | `User` | `§5` | Implementado |
| `TaxId` | formato fiscal simple documentado | `Seller` | `§5` | Implementado |
| `ProductVariant` | `type` + `value` no vacío | `Product` | `§5` | Implementado |
| `PasswordHash` | opaco, no vacío | `User` | `§5` | Implementado |
| `StockAllocation` | `productId`, `warehouseId`, `quantity > 0` | `Order` | `§5` | Implementado |

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
| R-03 entidades ricas | constructores/factorías, métodos de intención y colecciones inmutables | `DomainRichnessTest` + tests por agregado |
| R-04 sin Strings acotados | catálogos + `fromCode` en el borde | `RestMappersTest`, `CatalogConvertersTest` |
| R-05 dinero | `Money` + `BigDecimal` | `ValueObjectsTest` |
| R-06 stock | `Inventory.reserve/confirmSale/adjust`, stock nunca negativo y dañado no vendible | `InventoryTest`, `InventoryServiceTest` |
| R-07 pedido no modificable | `Order.assertModifiable` | `OrderTest`, `OrderServiceTest` |
| R-08 vendedor solo ADMIN | `Seller.registerBy` | `SellerTest`, `SellerServiceTest` |
| R-09 unicidad | índices únicos + servicios | `UserServiceTest`, `SellerServiceTest`, `JpaMappingConstraintsTest` |
| D-07 inventario | métodos y movimientos | `InventoryTest`, `InventoryServiceTest` |
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
- [x] 51 excepciones concretas + `DomainException` e `InvalidDataException` abstractas.
- [x] 6 prefijos REST con 64 endpoints.
- [x] Reglas R-01…R-12 y RG-01…RG-03 con evidencia y prueba.

## 12. Riesgos residuales y supuestos vigentes

| ID | Riesgo | Mitigación |
|---|---|---|
| D-09 | Matriz de responsabilidades | **Confirmada** contra PDF p.8 §12: Admin, Vendedor + Op. Logístico, Comprador + Vendedor + Op. Logístico, Comprador + Admin; las 4 columnas existen. Solo filas/roles ausentes quedan como supuesto. |
| A-16 | `AuthResult` como modelo de dominio (no VO) | Se mantiene en `domain.models`; el mapper REST lo convierte a `AuthResponse` |
| — | `Return` es palabra reservada contextual en Java | El modelo se llama `Return`; los mapeadores/adaptadores lo usan con nombre completo cuando haga falta |
| — | Enums sin `values()`/BOM (H-03/H-07) | Resuelto: catálogos sin BOM, con `fromCode` y `values` |

## 13. Integridad transaccional y consistencia (F3)

| Control | Implementación | Evidencia |
|---|---|---|
| Transacción por caso de uso | `@Transactional` de clase en los 6 `*UseCase` | `UseCaseTransactionTest` |
| Compensación de checkout | `OrderService.releaseReservations` en `catch` y stock insuficiente | `checkoutReleasesReservationsWhenOrderSaveFails`, `checkoutReleasesReservationsWhenStockIsInsufficient` |
| Despacho completo | `markDispatched` exige lista no vacía y `allMatch(IN_TRANSIT/DELIVERED)` por asignación | `dispatchRequiresAllAllocationsInTransit` |
| Entrega completa | `markDelivered` exige lista no vacía y `allMatch(DELIVERED)` por asignación | `deliveryRequiresEveryShipmentForAllocationToBeDelivered` |
| Autorización de inventario | `listBySeller`/`listByProduct` validan todos los registros del vendedor | `sellerCannotListAnotherSellersInventoryEvenWhenSellerHasNoProducts`, `sellerCannotListAnotherSellersInventoryByProduct` |
| Defectos y decisión 28/90 | `SDD/Gap-Analysis.md` | DEF-01…DEF-09; F4/F5 N/A sin respaldo PDF |

