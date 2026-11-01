# Puertos de salida (driven ports)

> Paquete: `application.domain.ports.out`.
> Abstraen persistencia y servicios externos. Usan exclusivamente tipos de
> dominio; los estados viajan como `OrderStatus`, `UserRole`, etc., nunca como
> `String` acotado. Devuelven `Optional` cuando la ausencia es normal.

## 1. Repositorios de agregados

### `UserRepositoryPort`

```java
package application.domain.ports.out;

import application.domain.enums.UserRole;
import application.domain.models.User;
import application.domain.valueobjects.Email;
import application.domain.valueobjects.IdentityDocument;
import java.util.List;
import java.util.Optional;

public interface UserRepositoryPort {
    User save(User user);
    Optional<User> findById(String id);
    Optional<User> findByEmail(Email email);
    List<User> findAll();
    boolean existsByEmail(Email email);
    boolean existsByIdentityDocument(IdentityDocument identityDocument);
    boolean existsByRole(UserRole role);
}
```

### `BuyerRepositoryPort`

```java
public interface BuyerRepositoryPort {
    Buyer save(Buyer buyer);
    Optional<Buyer> findById(String id);
    Optional<Buyer> findByUserId(String userId);
    List<Buyer> findAll();
}
```

### `SellerRepositoryPort`

```java
public interface SellerRepositoryPort {
    Seller save(Seller seller);
    Optional<Seller> findById(String id);
    Optional<Seller> findByUserId(String userId);
    List<Seller> findAll();
    boolean existsByTaxId(TaxId taxId);
}
```

### `WarehouseRepositoryPort`

```java
public interface WarehouseRepositoryPort {
    Warehouse save(Warehouse warehouse);
    Optional<Warehouse> findById(String id);
    List<Warehouse> findAll();
    List<Warehouse> findByOwnerSellerId(String sellerId);
    boolean existsByOwnerSellerId(String sellerId);
}
```

### `ProductRepositoryPort`

```java
public interface ProductRepositoryPort {
    Product save(Product product);
    Optional<Product> findById(String id);
    List<Product> findAll();
    List<Product> findPublished();
    List<Product> findBySellerId(String sellerId);
}
```

### `InventoryRepositoryPort`

```java
public interface InventoryRepositoryPort {
    Inventory save(Inventory inventory);
    Optional<Inventory> findById(String id);
    Optional<Inventory> findByProductIdAndWarehouseId(String productId, String warehouseId);
    List<Inventory> findByProductId(String productId);
    List<Inventory> findByWarehouseId(String warehouseId);
    List<Inventory> findBySellerId(String sellerId);
    List<Inventory> findAll();
    boolean existsByProductId(String productId);
}
```

### `InventoryMovementRepositoryPort`

```java
public interface InventoryMovementRepositoryPort {
    InventoryMovement save(InventoryMovement movement);
    List<InventoryMovement> findByInventoryId(String inventoryId);
    List<InventoryMovement> findAll();
}
```

### `CartRepositoryPort`

```java
public interface CartRepositoryPort {
    Cart save(Cart cart);
    Optional<Cart> findByBuyerId(String buyerId);
    void deleteByBuyerId(String buyerId);
}
```

### `OrderRepositoryPort`

```java
public interface OrderRepositoryPort {
    Order save(Order order);
    Optional<Order> findById(String id);
    List<Order> findAll();
    List<Order> findByBuyerId(String buyerId);
    List<Order> findBySellerId(String sellerId);
    List<Order> findByStatus(OrderStatus status);
    List<Order> findByBuyerIdAndStatus(String buyerId, OrderStatus status);
    List<Order> findByPaymentStatusAndCreatedAtBetween(
            PaymentStatus paymentStatus, Instant from, Instant to);
}
```

### `InvoiceRepositoryPort`

```java
public interface InvoiceRepositoryPort {
    Invoice save(Invoice invoice);
    Optional<Invoice> findById(String id);
    Optional<Invoice> findByOrderId(String orderId);
    List<Invoice> findByBuyerId(String buyerId);
    boolean existsByOrderId(String orderId);
}
```

### `ShipmentRepositoryPort`

```java
public interface ShipmentRepositoryPort {
    Shipment save(Shipment shipment);
    Optional<Shipment> findById(String id);
    List<Shipment> findByOrderId(String orderId);
    List<Shipment> findByOrderIdAndOriginWarehouseId(String orderId, String warehouseId);
    List<Shipment> findAll();
}
```

### `ReturnRepositoryPort`

```java
public interface ReturnRepositoryPort {
    Return save(Return returnRequest);
    Optional<Return> findById(String id);
    List<Return> findByOrderId(String orderId);
    List<Return> findByBuyerId(String buyerId);
    List<Return> findAll();
    List<Return> findByStatus(ReturnStatus status);
}
```

### `RefundRepositoryPort`

```java
public interface RefundRepositoryPort {
    Refund save(Refund refund);
    Optional<Refund> findById(String id);
    List<Refund> findByReturnId(String returnId);
    List<Refund> findByBuyerId(String buyerId);
    List<Refund> findAll();
}
```

## 2. Servicios externos

### `AuditPort` (MongoDB)

```java
package application.domain.ports.out;

import application.domain.enums.AuditAction;
import application.domain.models.AuditLog;
import java.time.LocalDate;
import java.util.List;

public interface AuditPort {
    void record(AuditLog auditLog);
    List<AuditLog> find(AuditAction action, LocalDate from, LocalDate to);
    List<AuditLog> findAll();
}
```

### `PasswordServicePort`

```java
public interface PasswordServicePort {
    PasswordHash encode(String rawPassword);
    boolean matches(String rawPassword, PasswordHash passwordHash);
}
```

Implementación: BCrypt en `infrastructure.security`.

### `JwtServicePort`

```java
public interface JwtServicePort {
    String issueToken(User user);
}
```

La **validación** del token (firma, expiración, `ver`, recarga del usuario) vive
en `infrastructure.security.JwtProvider` + `JwtAuthenticationFilter` (D-11); el
dominio solo emite.

### `TaxPolicyPort`

```java
public interface TaxPolicyPort {
    BigDecimal currentTaxRate();
}
```

Implementación: lee `nexusmarket.tax.rate` (por defecto `0.19`, D-12).

### `ClockPort`

```java
public interface ClockPort {
    Instant now();
    LocalDate today();
}
```

Implementación: `SystemClockAdapter` en `infrastructure.config`; los tests usan
un reloj fijo.

## 3. Implementaciones previstas (Fase 4)

| Puerto | Adaptador JPA (`adapters/persistence/jpa/adapters`) | Adaptador Mongo |
|---|---|---|
| `UserRepositoryPort` | `UserJpaAdapter` | — |
| `BuyerRepositoryPort` | `BuyerJpaAdapter` | — |
| `SellerRepositoryPort` | `SellerJpaAdapter` | — |
| `WarehouseRepositoryPort` | `WarehouseJpaAdapter` | — |
| `ProductRepositoryPort` | `ProductJpaAdapter` | — |
| `InventoryRepositoryPort` | `InventoryJpaAdapter` | — |
| `InventoryMovementRepositoryPort` | `InventoryMovementJpaAdapter` | — |
| `CartRepositoryPort` | `CartJpaAdapter` | — |
| `OrderRepositoryPort` | `OrderJpaAdapter` | — |
| `InvoiceRepositoryPort` | `InvoiceJpaAdapter` | — |
| `ShipmentRepositoryPort` | `ShipmentJpaAdapter` | — |
| `ReturnRepositoryPort` | `ReturnJpaAdapter` | — |
| `RefundRepositoryPort` | `RefundJpaAdapter` | — |
| `AuditPort` | — | `AuditLogMongoAdapter` |
| `PasswordServicePort` | `BCryptPasswordServiceAdapter` (security) | — |
| `JwtServicePort` | `JwtProvider` (security) | — |
| `TaxPolicyPort` | `PropertiesTaxPolicyAdapter` (config) | — |
| `ClockPort` | `SystemClockAdapter` (config) | — |
