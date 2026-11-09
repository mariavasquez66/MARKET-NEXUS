# API REST — contrato de endpoints

> Paquete: `application.adapters.rest.controllers`.
> Base: `/api/v1`. Todos los endpoints (salvo `/public/**`) exigen JWT.
> Los estados/tipos llegan como `String` en el borde y se convierten con
> `fromCode(...)`; un código inválido ⇒ 400.

## 1. `/api/v1/public` — `PublicController` (sin JWT)

| Método | Ruta | Request | Response | HTTP |
|---|---|---|---|---|
| GET | `/products` | — | `List<ProductResponse>` | 200 |
| GET | `/products/{id}` | — | `ProductResponse` | 200 / 404 |
| POST | `/buyers/register` | `RegisterBuyerRequest` | `UserResponse` | 201 / 400 / 409 |
| POST | `/auth/login` | `LoginRequest` | `AuthResponse` | 200 / 400 / 401 |

`AuthResponse` = `{ token, user }`. El token nunca incluye PII.

## 2. `/api/v1/buyer` — `BuyerController` (rol `BUYER`)

| Método | Ruta | Request | Response | HTTP |
|---|---|---|---|---|
| GET | `/cart` | — | `CartResponse` | 200 |
| POST | `/cart/items` | `AddCartItemRequest` | `CartResponse` | 200 / 400 / 404 / 409 |
| PUT | `/cart/items/{productId}` | `UpdateCartItemRequest` | `CartResponse` | 200 / 400 / 404 |
| DELETE | `/cart/items/{productId}` | — | — | 204 / 404 |
| DELETE | `/cart` | — | — | 204 |
| POST | `/orders` | `CheckoutRequest` | `OrderResponse` | 201 / 400 / 409 |
| GET | `/orders` | `?status=` (opcional) | `List<OrderResponse>` | 200 |
| GET | `/orders/{id}` | — | `OrderResponse` | 200 / 404 |
| POST | `/orders/{id}/pay` | — | `OrderResponse` | 200 / 404 / 409 |
| POST | `/orders/{id}/cancel` | — | `OrderResponse` | 200 / 404 / 409 |
| POST | `/orders/{id}/finalize` | — | `OrderResponse` | 200 / 404 / 409 |
| GET | `/orders/{id}/invoice` | — | `InvoiceResponse` | 200 / 404 |
| POST | `/orders/{id}/returns` | `RequestReturnRequest` | `ReturnResponse` | 201 / 400 / 409 |
| GET | `/returns` | — | `List<ReturnResponse>` | 200 |
| GET | `/refunds` | — | `List<RefundResponse>` | 200 |
| GET | `/addresses` | — | `List<AddressResponse>` | 200 |
| POST | `/addresses` | `AddAddressRequest` | `BuyerResponse` | 200 / 400 |
| PUT | `/addresses/main` | `ChangeMainAddressRequest` | `BuyerResponse` | 200 / 400 |

## 3. `/api/v1/seller` — `SellerController` (rol `SELLER`)

| Método | Ruta | Request | Response | HTTP |
|---|---|---|---|---|
| POST | `/products` | `CreateProductRequest` | `ProductResponse` | 201 / 400 / 409 |
| GET | `/products` | — | `List<ProductResponse>` | 200 |
| GET | `/products/{id}` | — | `ProductResponse` | 200 / 404 |
| PUT | `/products/{id}` | `UpdateProductRequest` | `ProductResponse` | 200 / 400 / 404 / 409 |
| POST | `/products/{id}/variants` | `AddVariantRequest` | `ProductResponse` | 200 / 400 / 404 |
| POST | `/products/{id}/publish` | — | `ProductResponse` | 200 / 404 / 409 |
| POST | `/products/{id}/suspend` | — | `ProductResponse` | 200 / 404 / 409 |
| POST | `/products/{id}/discontinue` | — | `ProductResponse` | 200 / 404 / 409 |
| POST | `/inventory` | `RegisterInventoryRequest` | `InventoryResponse` | 201 / 400 / 404 / 409 |
| GET | `/inventory` | — | `List<InventoryResponse>` | 200 |
| GET | `/orders` | — | `List<OrderResponse>` | 200 |

## 4. `/api/v1/logistics` — `LogisticsController` (rol `LOGISTIC_OPERATOR`)

| Método | Ruta | Request | Response | HTTP |
|---|---|---|---|---|
| GET | `/orders?status=` | `status` opcional | `List<OrderResponse>` | 200 / 400 |
| GET | `/orders/{id}/shipments` | — | `List<ShipmentResponse>` | 200 / 404 |
| GET | `/shipments/{id}` | — | `ShipmentResponse` | 200 / 404 |
| POST | `/shipments` | `CreateShipmentRequest` | `ShipmentResponse` | 201 / 400 / 404 / 409 |
| POST | `/shipments/{id}/start` | `StartShipmentRequest` | `ShipmentResponse` | 200 / 400 / 404 / 409 |
| POST | `/shipments/{id}/deliver` | — | `ShipmentResponse` | 200 / 404 / 409 |
| POST | `/inventory/{productId}/{warehouseId}/inflow` | `InventoryAdjustmentRequest` | `InventoryMovementResponse` | 200 / 400 / 404 / 409 |
| POST | `/inventory/{productId}/{warehouseId}/adjust` | `InventoryAdjustmentRequest` | `InventoryMovementResponse` | 200 / 400 / 404 / 409 |
| POST | `/inventory/{productId}/{warehouseId}/damaged` | — | `InventoryResponse` | 200 / 404 / 409 |
| POST | `/inventory/{productId}/{warehouseId}/repaired` | — | `InventoryResponse` | 200 / 404 / 409 |
| GET | `/returns` | — | `List<ReturnResponse>` | 200 |
| POST | `/returns/{id}/receive` | — | `ReturnResponse` | 200 / 404 / 409 |

## 5. `/api/v1/admin` — `AdminController` (rol `ADMIN`)

| Método | Ruta | Request | Response | HTTP |
|---|---|---|---|---|
| POST | `/sellers` | `RegisterSellerRequest` | `SellerResponse` | 201 / 400 / 403 / 409 |
| GET | `/sellers` | — | `List<SellerResponse>` | 200 |
| GET | `/sellers/{id}` | — | `SellerResponse` | 200 / 404 |
| PUT | `/sellers/{id}` | `UpdateSellerRequest` | `SellerResponse` | 200 / 400 / 404 / 409 |
| POST | `/warehouses` | `CreateWarehouseRequest` | `WarehouseResponse` | 201 / 400 / 404 |
| GET | `/warehouses` | — | `List<WarehouseResponse>` | 200 |
| GET | `/warehouses/{id}` | — | `WarehouseResponse` | 200 / 404 |
| PUT | `/warehouses/{id}` | `UpdateWarehouseRequest` | `WarehouseResponse` | 200 / 400 / 404 |
| POST | `/sellers/{id}/warehouses/{warehouseId}` | — | `SellerResponse` | 200 / 404 / 409 |
| GET | `/users` | — | `List<UserResponse>` | 200 |
| GET | `/users/{id}` | — | `UserResponse` | 200 / 404 |
| POST | `/users` | `CreateUserRequest` | `UserResponse` | 201 / 400 / 409 |
| POST | `/users/{id}/block` | — | `UserResponse` | 200 / 404 / 409 |
| POST | `/users/{id}/activate` | — | `UserResponse` | 200 / 404 / 409 |
| POST | `/users/{id}/deactivate` | — | `UserResponse` | 200 / 404 / 409 |
| GET | `/returns` | — | `List<ReturnResponse>` | 200 |
| POST | `/returns/{id}/approve` | — | `ReturnResponse` | 200 / 404 / 409 |
| POST | `/returns/{id}/reject` | — | `ReturnResponse` | 200 / 404 / 409 |
| POST | `/returns/{id}/refunds` | `GenerateRefundRequest` | `RefundResponse` | 201 / 400 / 404 / 409 |
| POST | `/refunds/{id}/process` | — | `RefundResponse` | 200 / 404 / 409 |

## 6. `/api/v1/supervisor` — `SupervisorController` (rol `SUPERVISOR`, solo `GET`)

| Método | Ruta | Query | Response |
|---|---|---|---|
| GET | `/reports/sales` | `from`, `to` (`yyyy-MM-dd`) | `SalesReportResponse` |
| GET | `/reports/orders` | — | `OrderSummaryResponse` |
| GET | `/reports/inventory` | — | `InventorySummaryResponse` |
| GET | `/reports/returns` | — | `ReturnSummaryResponse` |
| GET | `/reports/users` | — | `UserSummaryResponse` |
| GET | `/orders` | — | `List<OrderResponse>` |
| GET | `/audit-logs` | `action`, `from`, `to` (opcionales) | `List<AuditLogResponse>` |

## 7. Códigos de respuesta

| Código | Uso |
|---|---|
| 200 | consultas y acciones exitosas |
| 201 | creaciones (`POST /buyers/register`, `/orders`, `/products`, `/inventory`, `/shipments`, `/sellers`, `/warehouses`, `/users`, `/returns`, `/refunds`) |
| 204 | borrados (`DELETE /cart`, `DELETE /cart/items/{productId}`) |
| 400 | validación, códigos de catálogo inválidos, reglas de entrada |
| 401 | sin token, token inválido/expirado/obsoleto |
| 403 | rol incorrecto o recurso ajeno |
| 404 | recurso inexistente o reporte sin datos |
| 409 | conflicto de estado/unicidad |
| 500 | error no controlado (cuerpo genérico) |

## 8. DTOs previstos

**Request:** `RegisterBuyerRequest`, `LoginRequest`, `AddCartItemRequest`,
`UpdateCartItemRequest`, `CheckoutRequest`, `RequestReturnRequest`,
`AddAddressRequest`, `ChangeMainAddressRequest`, `CreateProductRequest`,
`UpdateProductRequest`, `AddVariantRequest`, `RegisterInventoryRequest`,
`CreateShipmentRequest`, `StartShipmentRequest`, `InventoryAdjustmentRequest`,
`RegisterSellerRequest`, `UpdateSellerRequest`, `CreateWarehouseRequest`,
`UpdateWarehouseRequest`, `CreateUserRequest`, `GenerateRefundRequest`.

**Response:** `AuthResponse`, `UserResponse`, `BuyerResponse`, `SellerResponse`,
`WarehouseResponse`, `AddressResponse`, `MoneyResponse`, `ProductResponse`,
`ProductVariantResponse`, `InventoryResponse`, `InventoryMovementResponse`,
`CartResponse`, `CartItemResponse`, `OrderResponse`, `OrderItemResponse`,
`StockAllocationResponse`, `InvoiceResponse`, `ShipmentResponse`,
`ReturnResponse`, `RefundResponse`, `SalesReportResponse`, `OrderSummaryResponse`,
`InventorySummaryResponse`, `ReturnSummaryResponse`, `UserSummaryResponse`,
`AuditLogResponse`, `ErrorResponse`.

Ningún DTO expone `passwordHash`, entidades JPA, documentos Mongo ni el hash de
contraseña. Los DTOs son `record` inmutables.
