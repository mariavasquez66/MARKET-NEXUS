# NexusMarket - Implementation Progress and Architecture Log

## Overview
This document tracks the incremental progress, domain entity enhancements, exception hierarchy, output ports, domain services, and architectural design decisions for the **NexusMarket** platform, implemented following Domain-Driven Design (DDD) and Hexagonal Architecture (Ports & Adapters).

---

## 1. Task 1 - Review and Correction of Existing Domain Entities & Value Objects

### 1.1 Corrected Value Objects & Entities
1. **Warehouse & WarehouseType**:
   - **Problem**: `Warehouse.type` was modeled as a raw `String` (`"MARKETPLACE"` / `"SELLER"`).
   - **Correction**: Created `WarehouseType` value object extending `DomainCatalog`, providing type-safe singletons `WarehouseType.MARKETPLACE` and `WarehouseType.SELLER` with parsing helper `fromCode(String)`. Replaced the String field in `Warehouse` with `WarehouseType`.

2. **Product & ProductStatus**:
   - **Problem**: `Product` had only a primitive `boolean published` flag and lacked an explicit catalog lifecycle status.
   - **Correction**: Created `ProductStatus` value object extending `DomainCatalog` with states `PUBLISHED`, `SUSPENDED`, and `DISCONTINUED`. Added `publish()`, `suspend()`, and `discontinue()` domain methods, as well as `isPublished()`, `isSuspended()`, and `isDiscontinued()` query methods.

3. **Product Variants**:
   - **Problem**: Functional specification (OBJ-05, Domain 5) defines `variants` (list of color, size, model, etc.), but `Product` did not have this attribute.
   - **Correction**: Added `List<String> variants` and method `addVariant(String)` to `Product`.

4. **Monetary Values (BigDecimal vs Double)**:
   - **Problem**: Monetary prices and amounts were declared as `Double` in `Product` (`price`), `Order` (`totalAmount`), `Invoice` (`subtotal`, `taxes`, `total`), and `Refund` (`amount`).
   - **Correction**: Converted all monetary fields to `BigDecimal` to ensure precision, avoid floating-point rounding errors, and comply strictly with architectural standards.

5. **Order Lifecycle & Cancellation**:
   - **Problem**: `OrderStatus` lacked `CANCELLED` status needed for `cancelOrder` business process, and `Order` lacked cancellation logic.
   - **Correction**: Added `OrderStatus.CANCELLED`. Implemented `Order.cancel()`, validating that finalized and delivered orders cannot be cancelled. Added helper queries `isCancelled()`, `isPaid()`, `isDispatched()`, `isDelivered()`.

6. **Buyer Commercial Lifecycle**:
   - **Problem**: `Buyer` had `restrict()` but lacked `activate()` to restore commercial status to `ACTIVE`.
   - **Correction**: Added `activate()`, `isActive()`, `isRestricted()`, and `updateMainAddress(String)`.

7. **Inventory Damage Tracking & Consistency**:
   - **Problem**: Specification critical validation states that damaged inventory cannot be reserved, but `Inventory` lacked damage status. Exception messages were also in Spanish.
   - **Correction**: Added `damaged` boolean flag, `markAsDamaged()`, `markAsRepaired()`, and `isDamaged()`. Added validation in `reserve()` preventing reservation of damaged inventory. Translated all internal domain messages to 100% English.

8. **Invoice Voiding**:
   - **Problem**: Invoicing rules allow voiding an invoice (`anularFactura`), but `Invoice` lacked void state.
   - **Correction**: Added `voided` boolean flag, `voidInvoice()`, and `isVoided()`.

9. **Refund Processing**:
   - **Problem**: `Refund` lacked state tracking for whether a refund was already processed.
   - **Correction**: Added `processed` boolean flag, `process()`, and `isProcessed()`.

10. **User Logical Deletion & State Checks**:
    - **Problem**: `User` had no logical deletion capability and lacked helper methods to check blocked/inactive status.
    - **Correction**: Added `deleted` boolean flag, `deactivate()`, `delete()`, `isDeleted()`, `isBlocked()`, `isInactive()`.

11. **Value Objects Enhancement**:
    - Added `fromCode(String code)` factory methods to all `DomainCatalog` subclasses (`UserRole`, `UserStatus`, `CommercialStatus`, `ProductType`, `ProductStatus`, `OrderStatus`, `PaymentStatus`, `ReturnStatus`, `ShipmentStatus`, `InventoryMovementType`, `WarehouseType`) for safe lookups from incoming strings without using raw strings in the domain model.

---

## 2. Task 2 & 3 - Domain Exceptions, Ports, and Domain Services

### 2.1 Common & Cross-Cutting Exceptions (`application.domain.exceptions.common`)
- **`DomainException`**: Abstract base runtime exception for all domain violations across NexusMarket.
- **`InvalidDataException`**: Thrown when provided inputs/payloads fail domain boundary validations (null, empty, negative, etc.).
- **`DuplicateEntityException`**: Base exception thrown when an entity already exists with the given unique identifier.
- **`UnauthenticatedUserException`**: Thrown when an operation lacks authenticated context (RG-01).
- **`SingleRoleException`**: Thrown when attempting to assign multiple roles to a user (RG-02).
- **`InsufficientPermissionException`**: Thrown when a user attempts an action outside their role privileges (RG-03, Responsibility Matrix).
- **`UnauthorizedOperationException`**: Thrown when an operation is prohibited by business state or security matrix.
- **`InvalidDateRangeException`**: Thrown when query or report date ranges are inverted or invalid.
- **`NoDataException`**: Thrown when a query or analytical report returns zero records.

---

### 2.2 User Domain (`application.domain.services.user`, `application.domain.exceptions.user`)
- **Exceptions Created**:
  - `UserNotFoundException`: Raised when looking up a non-existent user identifier.
  - `DuplicateEmailException`: Raised when attempting to register or update an email that already exists in the system.
  - `DuplicateIdentificationException`: Raised when a user ID/identification document is already registered.
  - `InactiveUserException`: Raised when an inactive or blocked user attempts restricted operations.
  - `InvalidRoleException`: Raised when role assignment is missing, invalid, or violates RG-02.
- **Output Port**:
  - `UserRepositoryPort`: Outbound interface abstracting persistence operations (`save`, `findById`, `findByEmail`, `existsById`, `existsByEmail`, `findAll`, `deleteById`).
- **Domain Service**:
  - `UserService`: Implements OBJ-01 (User Administration).
    - `createUser(User)`: Validates mandatory attributes, ID and email uniqueness, role assignment (RG-02), and persists the user.
    - `getUserById(String)`: Enforces non-blank validation and entity existence check.
    - `getAllUsers()`: Retrieves all registered users.
    - `updateUser(String, String, String)`: Allows modification of name and email (with conflict checking), while keeping the user role immutable.
    - `blockUser(String)`: Invokes domain behavior `user.block()` and saves.
    - `activateUser(String)`: Invokes domain behavior `user.activate()` and saves.
    - `deleteUser(String)`: Executes logical deletion via `user.delete()` and saves.

---

### 2.3 Buyer Domain (`application.domain.services.buyer`, `application.domain.exceptions.buyer`)
- **Exceptions Created**:
  - `BuyerNotFoundException`: Raised when searching for a non-existent buyer.
  - `RestrictedBuyerException`: Raised when a buyer with RESTRICTED commercial status attempts unauthorized purchases.
  - `InvalidAddressException`: Raised when provided delivery addresses are blank or invalid.
- **Output Port**:
  - `BuyerRepositoryPort`: Interface for buyer persistence operations (`save`, `findById`, `findByUserId`, `existsById`, `existsByUserId`, `findAll`).
- **Domain Service**:
  - `BuyerService`: Implements OBJ-03 (Buyer Management).
    - `registerBuyer(String id, String userId, String mainAddress)`: Validates that the associated user exists, is active, and possesses the `BUYER` role exclusively. Enforces unique buyer profile per user.
    - `getBuyerById(String)`: Retrieves a buyer by ID with existence validation.
    - `getAllBuyers()`: Retrieves all buyers.
    - `addAdditionalAddress(String, String)`: Adds a non-blank secondary delivery address.
    - `updateMainAddress(String, String)`: Updates the primary delivery address.
    - `restrictBuyer(String)`: Transitions commercial status to `RESTRICTED`.
    - `activateBuyer(String)`: Restores commercial status to `ACTIVE`.

---

### 2.4 Seller Domain (`application.domain.services.seller`, `application.domain.exceptions.seller`)
- **Exceptions Created**:
  - `SellerNotFoundException`: Raised when looking up a non-existent seller.
  - `SelfRegistrationNotAllowedException`: Enforces the business rule that sellers cannot self-register.
  - `SellerWithoutWarehouseException`: Raised when an operation requires an assigned warehouse but seller has none.
  - `WarehouseAlreadyAssignedException`: Raised when attempting to assign a warehouse already linked to another seller.
  - `WarehouseNotFoundException`: Raised when the target warehouse for assignment is not found.
- **Output Ports**:
  - `SellerRepositoryPort`: Outbound interface for seller storage operations (`save`, `findById`, `findByUserId`, `findByTaxId`, `existsById`, `existsByUserId`, `existsByTaxId`, `findAll`).
  - `WarehouseRepositoryPort`: Outbound interface for warehouse storage operations (`save`, `findById`, `findAll`, `findBySellerId`, `existsById`).
- **Domain Service**:
  - `SellerService`: Implements OBJ-02 (Seller Management).
    - `registerSeller(String id, String userId, String taxId, String adminUserId)`: Strictly requires an authenticated `ADMIN` user to register sellers. Blocks self-registration attempts. Validates tax ID uniqueness and associated user status/role.
    - `getSellerById(String)`: Retrieves a seller with existence validation.
    - `getAllSellers()`: Lists all registered sellers.
    - `updateSeller(String, String)`: Updates tax ID while ensuring tax ID uniqueness.
    - `assignWarehouse(String, String)`: Links a warehouse to a seller and marks the warehouse type as `SELLER`, preventing conflicting assignments.
    - `validateSellerHasWarehouses(String)`: Validates that a seller has at least one associated warehouse before allowing product publishing or inventory intake.

---

### 2.5 Warehouse Domain (`application.domain.services.warehouse`, `application.domain.exceptions.warehouse`)
- **Exceptions Created**:
  - `WarehouseNotFoundException`: Raised when looking up a warehouse by identifier that does not exist.
  - `InvalidWarehouseTypeException`: Raised when an invalid or null warehouse type is provided.
  - `WarehouseAlreadyAssignedException`: Raised when attempting to assign a warehouse already belonging to another seller.
- **Output Port**:
  - `WarehouseRepositoryPort`: Outbound interface managing warehouse queries and persistence (`save`, `findById`, `findAll`, `findBySellerId`, `existsById`).
- **Domain Service**:
  - `WarehouseService`: Implements OBJ-04 (Warehouse Management).
    - `createWarehouse(String id, String name, String address, WarehouseType type, String sellerId)`: Enforces non-blank address and name, validates type, ensures unique ID, associates with seller if `SELLER` type, and updates the seller's warehouse list.
    - `getWarehouseById(String)`: Retrieves warehouse by ID.
    - `getAllWarehouses()`: Lists all warehouses across marketplace and sellers.
    - `updateWarehouse(String, String, String)`: Updates warehouse metadata (name, physical address).
    - `listWarehousesBySeller(String)`: Retrieves warehouses assigned to a specific verified seller.

---

### 2.6 Catalog & Product Domain (`application.domain.services.product`, `application.domain.exceptions.product`)
- **Exceptions Created**:
  - `ProductNotFoundException`: Raised when searching for a non-existent product ID.
  - `ProductNotPublishedException`: Raised when trying to access or order an unpublished product.
  - `ProductAlreadyPublishedException`: Raised when attempting to publish a product that is already in `PUBLISHED` status.
  - `InvalidPriceException`: Raised when product price is negative or zero (violating positive price rule).
  - `InvalidProductTypeException`: Raised when product type is missing or not recognized.
- **Output Port**:
  - `ProductRepositoryPort`: Outbound interface managing product persistence (`save`, `findById`, `findAll`, `findByStatus`, `findBySellerId`, `existsById`, `deleteById`).
- **Domain Service**:
  - `CatalogService`: Implements OBJ-05 (Catalog Management).
    - `registerProduct(...)`: Enforces price > 0 (BigDecimal), validates product type, ensures existence of seller, and validates that sellers of physical products have at least one registered warehouse before registering products.
    - `getProductById(String)`: Retrieves product by ID.
    - `listPublishedProducts()`: Returns all products visible to buyers (`ProductStatus.PUBLISHED`).
    - `listProductsBySeller(String)`: Returns all products offered by a given seller.
    - `updateProduct(...)`: Allows modification of descriptive fields, price (revalidating positivity), and variants.
    - `publishProduct(String)`: Changes status from `SUSPENDED` to `PUBLISHED` with physical product warehouse checks.
    - `hideProduct(String)`: Temporarily suspends the product from public view.
    - `discontinueProduct(String)`: Permanently discontinues the product.
    - `deleteProduct(String)`: Executes logical removal of the product from the catalog.

---

### 2.7 Inventory Domain (`application.domain.services.inventory`, `application.domain.exceptions.inventory`)
- **Exceptions Created**:
  - `InventoryNotFoundException`: Raised when inventory does not exist for a given product and warehouse pair.
  - `InsufficientStockException`: Raised when requested units exceed available unreserved stock.
  - `NegativeStockException`: Raised when an intake or adjustment results in negative stock, strictly prohibited by the specification.
  - `DamagedInventoryException`: Enforces the critical validation that damaged stock cannot be reserved for orders.
  - `InvalidReservationException`: Raised when trying to cancel more reserved units than currently held.
  - `InvalidAdjustmentException`: Raised when an inventory correction fails domain constraints.
- **Output Port**:
  - `InventoryRepositoryPort`: Outbound interface managing inventory records (`save`, `findById`, `findByProductIdAndWarehouseId`, `findByProductId`, `findByWarehouseId`, `findAll`, `existsByProductIdAndWarehouseId`).
- **Domain Service**:
  - `InventoryService`: Implements OBJ-06 (Distributed Inventory Management).
    - `registerInventory(String, String, int)`: Enforces mandatory distributed link between a valid product and a valid warehouse.
    - `getInventory(String, String)`: Retrieves inventory record for product/warehouse pair.
    - `addStock(String, String, int)`: Executes INGRESS movement.
    - `reserveStock(String, String, int)`: Executes RESERVATION movement, strictly verifying stock is not marked as damaged and is sufficient.
    - `confirmSale(String, String, int)`: Executes SALE movement, deducting from both total and reserved quantities.
    - `cancelReservation(String, String, int)`: Releases previously reserved stock back to available pool.
    - `applyAdjustment(String, String, int)`: Executes ADJUSTMENT movement, guaranteeing stock never falls below zero.
    - `registerReturn(String, String, int)`: Executes RETURN movement, re-incorporating units into inventory.
    - `getAvailableStock(String, String)`: Computes `quantity - reservedQuantity` (or 0 if damaged).
    - `markAsDamaged(String, String)` / `markAsRepaired(String, String)`: Controls quality status of inventory units.

---

### 2.8 Shopping Cart Domain (`application.domain.services.cart`, `application.domain.exceptions.cart`)
- **Exceptions Created**:
  - `CartNotFoundException`: Raised when looking up an active shopping cart for a buyer that does not exist.
  - `EmptyCartException`: Enforces the rule that an empty cart cannot be converted into an order.
  - `ProductNotAvailableException`: Raised when adding an unpublished or retired product to the cart.
  - `InvalidQuantityException`: Raised when item quantity is zero or negative.
- **Output Ports**:
  - `CartRepositoryPort`: Outbound interface managing cart persistence (`save`, `findById`, `findByBuyerId`, `deleteByBuyerId`).
  - `OrderRepositoryPort`: Outbound interface managing order persistence (`save`, `findById`, `findByBuyerId`, `findBySellerId`, `findByStatus`, `findAll`, `existsById`).
- **Domain Service**:
  - `CartService`: Implements OBJ-07 (Shopping Cart Management).
    - `createCart(String)`: Instantiates or fetches the active shopping cart for a buyer, checking buyer existence and unrestricted status.
    - `getActiveCart(String)`: Retrieves active shopping cart.
    - `addItem(String, String, int)`: Validates positive quantity, product availability (`PUBLISHED` status), buyer commercial status, and adds product to cart.
    - `removeItem(String, String)`: Removes product from cart.
    - `updateQuantity(String, String, int)`: Updates item quantity or removes item if 0.
    - `clearCart(String)`: Clears all items in the cart.
    - `convertToOrder(String)`: Converts non-empty cart items into a newly persisted `Order` in `PENDING_PAYMENT` status, computes total with `BigDecimal` precision, and clears the cart.

---

### 2.9 Order Domain (`application.domain.services.order`, `application.domain.exceptions.order`)
- **Exceptions Created**:
  - `OrderNotFoundException`: Raised when searching for a non-existent order.
  - `InvalidStateTransitionException`: Enforces valid order progression through lifecycle stages.
  - `FinalizedOrderException`: Enforces the critical business rule that finalized orders cannot be altered or cancelled under any circumstance.
  - `OrderAlreadyPaidException`: Raised when attempting to confirm payment on an already paid order.
  - `OrderNotPaidException`: Raised when dispatch is attempted on an unpaid order.
  - `OrderNotDispatchedException`: Raised when delivery confirmation is attempted on a non-dispatched order.
  - `OrderNotDeliveredException`: Raised when finalization or returns are attempted on a non-delivered order.
  - `CancelledOrderException`: Raised when modifying an order that has been cancelled.
- **Output Port**:
  - `OrderRepositoryPort`: Outbound interface managing order queries and persistence (`save`, `findById`, `findByBuyerId`, `findBySellerId`, `findByStatus`, `findAll`, `existsById`).
- **Domain Service**:
  - `OrderService`: Implements OBJ-08 (Order Lifecycle Management).
    - `createOrder(String, Map<String, Integer>)`: Creates order in `PENDING_PAYMENT` status, validating buyer status, published status of each product, positive quantities, and calculating `totalAmount` in `BigDecimal`.
    - `getOrderById(String)`: Retrieves order with existence checks.
    - `listOrdersByBuyer(String)`: Lists all orders placed by a buyer.
    - `listOrdersBySeller(String)`: Lists all orders containing items from a seller.
    - `confirmPayment(String)`: Enforces `PENDING_PAYMENT -> PAID` transition, updating payment status to `CONFIRMED`.
    - `dispatchOrder(String)`: Enforces `PAID -> DISPATCHED` transition.
    - `deliverOrder(String)`: Enforces `DISPATCHED -> DELIVERED` transition.
    - `finalizeOrder(String)`: Enforces `DELIVERED -> FINALIZED` transition, locking the order permanently.
    - `cancelOrder(String)`: Cancels orders prior to delivery/finalization/payment, transitioning status to `CANCELLED`.

---

### 2.10 Invoicing Domain (`application.domain.services.invoice`, `application.domain.exceptions.invoice`)
- **Exceptions Created**:
  - `InvoiceNotFoundException`: Raised when searching for an invoice that does not exist.
  - `InvoiceAlreadyExistsException`: Enforces the rule that each paid order can have at most one issued invoice.
  - `VoidedInvoiceException`: Raised when attempting operations on an already voided invoice.
- **Output Port**:
  - `InvoiceRepositoryPort`: Outbound interface managing invoice persistence (`save`, `findById`, `findByOrderId`, `findByBuyerId`, `findAll`, `existsByOrderId`).
- **Domain Service**:
  - `InvoiceService`: Implements OBJ-09 (Billing and Invoicing Management).
    - `generateInvoice(String)`: Validates that the order exists, is in `PAID` status, and has no prior invoice. Computes taxes and subtotal using `BigDecimal`, assigns a unique sequential invoice number, and persists the invoice.
    - `getInvoiceById(String)`: Retrieves invoice by ID.
    - `getInvoiceByOrder(String)`: Retrieves the invoice linked to a specific order.
    - `listInvoicesByBuyer(String)`: Lists all invoices belonging to a buyer.
    - `voidInvoice(String)`: Marks an existing invoice as voided (`voided = true`), preventing repeated cancellation.

---

### 2.11 Logistics Domain (`application.domain.services.logistics`, `application.domain.exceptions.logistics`)
- **Exceptions Created**:
  - `ShipmentNotFoundException`: Raised when searching for a non-existent shipment.
  - `ShipmentAlreadyStartedException`: Raised when trying to dispatch a shipment already in transit or delivered.
  - `ShipmentAlreadyDeliveredException`: Raised when performing transitions on an already delivered shipment.
  - `InvalidDestinationAddressException`: Raised when destination address is blank or invalid.
- **Output Port**:
  - `ShipmentRepositoryPort`: Outbound interface managing shipment persistence (`save`, `findById`, `findByOrderId`, `findAll`, `existsById`).
- **Domain Service**:
  - `LogisticsService`: Implements OBJ-10 (Logistics and Fulfillment Management).
    - `createShipment(String, String, String)`: Validates that the order exists and is in `PAID` status, verifies warehouse existence, validates non-blank delivery destination, and initiates shipment in `PENDING` status.
    - `getShipmentById(String)`: Retrieves shipment by identifier.
    - `listShipmentsByOrder(String)`: Retrieves all shipment legs associated with an order.
    - `startShipping(String, String)`: Assigns tracking number and transitions shipment to `IN_TRANSIT`.
    - `confirmDelivery(String)`: Confirms delivery of shipment, transitioning state to `DELIVERED`.
    - `updateShipmentStatus(String, ShipmentStatus)`: Updates shipment lifecycle status.

---

### 2.12 Return Domain (`application.domain.services.returns`, `application.domain.exceptions.returns`)
- **Exceptions Created**:
  - `ReturnNotFoundException`: Raised when searching for a non-existent return request.
  - `ReturnAlreadyProcessedException`: Raised when attempting to approve or reject a return request that has already passed the `REQUESTED` stage.
  - `ReturnNotApprovedException`: Raised when trying to complete a return or issue a refund for a non-approved return.
- **Output Port**:
  - `ReturnRepositoryPort`: Outbound interface managing return request persistence (`save`, `findById`, `findByOrderId`, `findAll`, `existsById`).
- **Domain Service**:
  - `ReturnService`: Implements OBJ-11 (Returns Management).
    - `requestReturn(String, String)`: Strictly verifies that the order exists and is in `DELIVERED` or `FINALIZED` status before accepting a return request with non-blank reason.
    - `getReturnById(String)`: Retrieves return request by ID.
    - `listReturnsByOrder(String)`: Retrieves all returns associated with an order.
    - `approveReturn(String)`: Moves return status from `REQUESTED` to `APPROVED`.
    - `rejectReturn(String)`: Moves return status from `REQUESTED` to `REJECTED`.
    - `completeReturn(String)`: Finalizes an approved return, transitioning status to `COMPLETED`.

---

### 2.13 Refund Domain (`application.domain.services.refund`, `application.domain.exceptions.refund`)
- **Exceptions Created**:
  - `RefundNotFoundException`: Raised when searching for a non-existent refund.
  - `InvalidAmountException`: Raised when refund amount is non-positive or exceeds the original order value.
  - `RefundAlreadyProcessedException`: Raised when trying to process a refund that has already completed.
- **Output Port**:
  - `RefundRepositoryPort`: Outbound interface managing refund persistence (`save`, `findById`, `findByReturnId`, `findAll`, `existsById`).
- **Domain Service**:
  - `RefundService`: Implements OBJ-11 (Refund Management).
    - `generateRefund(String, BigDecimal, String)`: Validates that the return request exists, is in `APPROVED` or `COMPLETED` status, ensures amount > 0, verifies amount does not exceed the original order total amount, and records refund in pending processed state.
    - `getRefundById(String)`: Retrieves refund by ID.
    - `listRefundsByReturn(String)`: Lists all refunds generated for a specific return.
    - `processRefund(String)`: Marks a pending refund as processed (`processed = true`).

---

### 2.14 Report Domain (`application.domain.services.report`, `application.domain.exceptions.common`)
- **Report Models Created**:
  - `SalesReport`: Encapsulates total revenue (`BigDecimal`), order volume, and unit sales over an evaluated date range.
  - `OrderSummary`: Summarizes total orders and order distribution categorized by lifecycle status.
  - `InventorySummary`: Aggregates total stock, reserved inventory, net available stock, and damaged inventory count.
  - `ReturnSummary`: Details total returns count, breakdown by status, total refunded amount, and processed refund counts.
  - `UserSummary`: Categorizes registered users by role, active counts, and blocked counts.
- **Domain Service**:
  - `ReportService`: Implements OBJ-12 (Administrative Reporting and Analytics).
    - `getSalesReport(LocalDate, LocalDate)`: Validates valid date bounds (raising `InvalidDateRangeException` if inverted), computes total revenue and sales volumes, and raises `NoDataException` when no matching non-cancelled orders exist.
    - `getOrderSummary()`: Aggregates order lifecycle counts or raises `NoDataException` if empty.
    - `getInventorySummary()`: Calculates network-wide distributed inventory metrics or raises `NoDataException`.
    - `getReturnSummary()`: Calculates return statuses and processed monetary refunds or raises `NoDataException`.
    - `getUserSummary()`: Aggregates system user statistics across roles and operational states.

---

## 3. Automated Test Verification (`src/test/java/application/domain/DomainServicesTest.java`)

A comprehensive unit test suite (`DomainServicesTest`) was implemented to systematically test the domain invariants, exception handling, and business workflows across all 13 domain services:
- **`testUserManagement()`**: Verifies unique email/ID constraints, active/blocked transitions, and role enforcement.
- **`testBuyerManagement()`**: Verifies registration, address updates, and restricted status behavior.
- **`testSellerAndWarehouseManagement()`**: Verifies seller warehouse assignment, disallowing self-registration, and warehouse type handling (`MARKETPLACE` vs `SELLER`).
- **`testCatalogAndInventory()`**: Verifies product publishing/suspension lifecycle, stock reservation, stock reduction, damaged inventory reservation prohibition, and negative stock prevention.
- **`testCartAndOrderLifecycle()`**: Verifies adding items to shopping carts, checkout conversion to orders, stock checks, and strict state transitions (`PENDING -> PAID -> DISPATCHED -> DELIVERED -> FINALIZED`).
- **`testInvoiceLogisticsReturnsAndRefunds()`**: Verifies invoice generation on paid orders, invoice voiding, logistics tracking number assignment and delivery, return approval requirement, and refund bounds checking.
- **`testReportingService()`**: Verifies aggregated sales calculations, inventory summaries, and date range boundary validations.

**Execution Result**:
```text
[INFO] Running application.domain.DomainServicesTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.129 s -- in application.domain.DomainServicesTest
[INFO] Results:
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

## 4. Conventional Commits Log

| Commit | Scope | Description |
|---|---|---|
| `8e9ad2c` | `fix(domain)` | Refactor entities and value objects to align with DDD and functional specification (`WarehouseType`, `ProductStatus`, `variants`, `BigDecimal` monetary values, `OrderStatus.CANCELLED`, domain methods) |
| `45056dd` | `feat(exceptions)` | Add base `DomainException` and common domain exceptions (`RG-01`, `RG-02`, `RG-03`, `InvalidDataException`, `NoDataException`, etc.) |
| `5cfa207` | `feat(user)` | Add user domain exceptions, `UserRepositoryPort`, and `UserService` implementation |
| `0429f41` | `feat(buyer)` | Add buyer domain exceptions, `BuyerRepositoryPort`, and `BuyerService` implementation |
| `6f9afc0` | `feat(seller)` | Add seller exceptions, ports, and `SellerService` implementation |
| `827d87f` | `feat(warehouse)` | Add warehouse exceptions and `WarehouseService` implementation |
| `ee78c5a` | `feat(catalog)` | Add product exceptions, `ProductRepositoryPort`, and `CatalogService` implementation |
| `1ab6433` | `feat(inventory)` | Add inventory exceptions, `InventoryRepositoryPort`, and `InventoryService` implementation |
| `6375ef7` | `feat(cart)` | Add shopping cart exceptions, ports, and `CartService` implementation |
| `aada3b0` | `feat(order)` | Add order exceptions and `OrderService` implementation |
| `6a950c1` | `feat(invoice)` | Add invoice exceptions, `InvoiceRepositoryPort`, and `InvoiceService` implementation |
| `82676a2` | `feat(logistics)` | Add logistics exceptions, `ShipmentRepositoryPort`, and `LogisticsService` implementation |
| `97953d4` | `feat(returns)` | Add return exceptions, `ReturnRepositoryPort`, and `ReturnService` implementation |
| `2ff39d0` | `feat(refund)` | Add refund exceptions, `RefundRepositoryPort`, and `RefundService` implementation |
| `a9a97d2` | `feat(report)` | Add report models and `ReportService` implementation |
| `fbb71c2` | `test(domain)` | Add comprehensive unit test suite and test configuration for all domain services |

---

## 5. Architectural Considerations and Business Rules for Review

1. **Multi-Seller Order Fulfillment & Split Shipments**:
   - *Current Implementation*: An order is associated with items that may originate from different sellers or warehouses. Logistics shipment generation (`createShipment`) currently binds an order to a warehouse destination leg.
   - *Consideration for Decision*: In future iterations, orders containing items across multiple warehouses/sellers can be partitioned into child fulfillment orders or multiple `Shipment` entities per warehouse automatically.
2. **Tax Computation Rules**:
   - *Current Implementation*: Applied a standard 19% VAT rate (`BigDecimal("0.19")`) during invoice generation in `InvoiceService`.
   - *Consideration for Decision*: If taxes vary by product category, regional tax jurisdiction, or exemption rules, a dedicated `TaxCalculationPolicy` domain service should be injected.
3. **Inventory Replenishment & Automatic Reorder Triggers**:
   - *Current Implementation*: Invariants prevent negative stock, prevent reserving damaged units, and manage reservation/release states cleanly.
   - *Consideration for Decision*: Automatic threshold alerting or reorder triggers when stock falls below `minimumStock` can be hooked in via domain events (e.g., `StockThresholdReachedEvent`).
4. **Order Cancellation Window & Stock Rollback**:
   - *Current Implementation*: `OrderService.cancelOrder()` transitions order status to `CANCELLED` if not finalized or delivered.
   - *Consideration for Decision*: Depending on whether stock was hard-deducted or merely reserved, a domain event or saga can orchestrate releasing reserved stock back to `InventoryService`.
