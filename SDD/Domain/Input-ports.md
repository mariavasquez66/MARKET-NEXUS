# Puertos de entrada (driving ports)

> Paquete: `application.domain.ports.in`.
> Los seis puertos se organizan por rol. **No se crean**
> `CommercialEmployeePort` ni `InternalAnalystPort` (D-10: venían de la
> plantilla bancaria y no aplican a NexusMarket).
>
> Contrato común: el `User` autenticado se pasa como **primer parámetro** en
> todos los métodos, salvo en `PublicAccessPort`. Las firmas usan exclusivamente
> tipos de dominio (`User`, `Product`, `Order`, `Money`, `Address`, Enums/VOs),
> nunca `String` para estados ni tipos acotados.

## 1. `PublicAccessPort` (sin JWT)

```java
package application.domain.ports.in;

import application.domain.models.AuthResult;
import application.domain.models.Product;
import application.domain.models.User;
import application.domain.valueobjects.Address;
import application.domain.valueobjects.Email;
import application.domain.valueobjects.FullName;
import application.domain.valueobjects.IdentityDocument;
import java.util.List;

public interface PublicAccessPort {

    List<Product> listPublishedProducts();

    Product getPublishedProduct(String productId);

    User registerBuyer(FullName fullName,
                       Email email,
                       IdentityDocument identityDocument,
                       String rawPassword,
                       Address mainAddress);

    AuthResult login(String email, String rawPassword);
}
```

## 2. `NaturalCustomerPort` (rol `BUYER`)

```java
package application.domain.ports.in;

import application.domain.enums.OrderStatus;
import application.domain.models.Buyer;
import application.domain.models.Cart;
import application.domain.models.Invoice;
import application.domain.models.Order;
import application.domain.models.Refund;
import application.domain.models.Return;
import application.domain.models.User;
import application.domain.valueobjects.Address;
import java.util.List;

public interface NaturalCustomerPort {

    Cart getCart(User actor);

    Cart addCartItem(User actor, String productId, int quantity);

    Cart updateCartItemQuantity(User actor, String productId, int quantity);

    Cart removeCartItem(User actor, String productId);

    void clearCart(User actor);

    Order checkout(User actor, Address deliveryAddress);

    List<Order> listOrders(User actor);

    List<Order> listOrdersByStatus(User actor, OrderStatus status);

    Order getOrder(User actor, String orderId);

    Order payOrder(User actor, String orderId);

    Order cancelOrder(User actor, String orderId);

    Invoice getInvoice(User actor, String orderId);

    Return requestReturn(User actor, String orderId, String reason);

    List<Return> listReturns(User actor);

    List<Refund> listRefunds(User actor);

    Buyer addAddress(User actor, Address address);

    Buyer changeMainAddress(User actor, Address address);
}
```

## 3. `BusinessCustomerPort` (rol `SELLER`)

```java
package application.domain.ports.in;

import application.domain.enums.ProductType;
import application.domain.enums.VariantType;
import application.domain.models.Inventory;
import application.domain.models.Order;
import application.domain.models.Product;
import application.domain.models.User;
import application.domain.valueobjects.Money;
import java.util.List;

public interface BusinessCustomerPort {

    Product registerProduct(User actor, String name, String description,
                            ProductType type, Money price);

    List<Product> listProducts(User actor);

    Product updateProduct(User actor, String productId, String name,
                          String description, Money price);

    Product addVariant(User actor, String productId, VariantType type, String value);

    Product publishProduct(User actor, String productId);

    Product suspendProduct(User actor, String productId);

    Product discontinueProduct(User actor, String productId);

    Inventory registerInventory(User actor, String productId,
                                String warehouseId, int initialQuantity);

    List<Inventory> listInventory(User actor);

    List<Order> listOrders(User actor);
}
```

## 4. `BusinessOperatorPort` (rol `LOGISTIC_OPERATOR`)

```java
package application.domain.ports.in;

import application.domain.enums.OrderStatus;
import application.domain.models.Inventory;
import application.domain.models.InventoryMovement;
import application.domain.models.Order;
import application.domain.models.Return;
import application.domain.models.Shipment;
import application.domain.models.User;
import java.util.List;

public interface BusinessOperatorPort {

    List<Order> listOrders(User actor, OrderStatus status);

    Shipment createShipment(User actor, String orderId, String warehouseId);

    Shipment startShipping(User actor, String shipmentId, String trackingNumber);

    Shipment confirmDelivery(User actor, String shipmentId);

    InventoryMovement registerInflow(User actor, String productId,
                                     String warehouseId, int quantity);

    InventoryMovement adjustInventory(User actor, String productId,
                                      String warehouseId, int delta);

    Inventory markDamaged(User actor, String productId, String warehouseId);

    Inventory markRepaired(User actor, String productId, String warehouseId);

    List<Return> listReturns(User actor);

    Return receiveReturn(User actor, String returnId);
}
```

## 5. `BusinessSupervisorPort` (rol `SUPERVISOR`, solo lectura)

```java
package application.domain.ports.in;

import application.domain.enums.AuditAction;
import application.domain.models.AuditLog;
import application.domain.models.Order;
import application.domain.models.User;
import application.domain.models.reports.InventorySummary;
import application.domain.models.reports.OrderSummary;
import application.domain.models.reports.ReturnSummary;
import application.domain.models.reports.SalesReport;
import application.domain.models.reports.UserSummary;
import java.time.LocalDate;
import java.util.List;

public interface BusinessSupervisorPort {

    SalesReport salesReport(User actor, LocalDate from, LocalDate to);

    OrderSummary orderSummary(User actor);

    InventorySummary inventorySummary(User actor);

    ReturnSummary returnSummary(User actor);

    UserSummary userSummary(User actor);

    List<Order> listOrders(User actor);

    List<AuditLog> listAuditLogs(User actor, AuditAction action,
                                 LocalDate from, LocalDate to);
}
```

## 6. `TellerEmployeePort` (rol `ADMIN`)

```java
package application.domain.ports.in;

import application.domain.enums.UserRole;
import application.domain.enums.WarehouseType;
import application.domain.models.Refund;
import application.domain.models.Return;
import application.domain.models.Seller;
import application.domain.models.User;
import application.domain.models.Warehouse;
import application.domain.valueobjects.Address;
import application.domain.valueobjects.Email;
import application.domain.valueobjects.FullName;
import application.domain.valueobjects.IdentityDocument;
import application.domain.valueobjects.Money;
import application.domain.valueobjects.TaxId;
import java.util.List;

public interface TellerEmployeePort {

    Seller registerSeller(User actor, String userId, TaxId taxId);

    List<Seller> listSellers(User actor);

    Seller updateSellerTaxId(User actor, String sellerId, TaxId taxId);

    Seller assignWarehouse(User actor, String sellerId, String warehouseId);

    Warehouse createWarehouse(User actor, String name, Address location,
                              WarehouseType type, String ownerSellerId,
                              String responsibleUserId);

    List<Warehouse> listWarehouses(User actor);

    Warehouse updateWarehouse(User actor, String warehouseId, String name,
                              Address location, String responsibleUserId);

    List<User> listUsers(User actor);

    User createUser(User actor, FullName fullName, Email email,
                    IdentityDocument identityDocument, UserRole role,
                    String rawPassword);

    User blockUser(User actor, String userId);

    User activateUser(User actor, String userId);

    User deactivateUser(User actor, String userId);

    List<Return> listReturns(User actor);

    Return approveReturn(User actor, String returnId);

    Return rejectReturn(User actor, String returnId);

    Refund generateRefund(User actor, String returnId, Money amount);

    Refund processRefund(User actor, String refundId);
}
```

## 7. Casos de uso que implementan estos puertos

| Puerto | Caso de uso (`adapters/useCases`) | Controlador REST |
|---|---|---|
| `PublicAccessPort` | `PublicAccessUseCase` | `/api/v1/public` |
| `NaturalCustomerPort` | `NaturalCustomerUseCase` | `/api/v1/buyer` |
| `BusinessCustomerPort` | `BusinessCustomerUseCase` | `/api/v1/seller` |
| `BusinessOperatorPort` | `BusinessOperatorUseCase` | `/api/v1/logistics` |
| `BusinessSupervisorPort` | `BusinessSupervisorUseCase` | `/api/v1/supervisor` |
| `TellerEmployeePort` | `TellerEmployeeUseCase` | `/api/v1/admin` |

Los casos de uso solo orquestan servicios de dominio; no contienen lógica de
negocio ni acceso a repositorios.
