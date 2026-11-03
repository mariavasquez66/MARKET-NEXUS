# Validación REST y DTOs

> Paquete: `application.adapters.rest.dto.{request,response}`.
> Los DTOs son `record` inmutables con Jakarta Bean Validation. El dominio no
> depende de estas anotaciones.

## 1. Reglas generales

1. **Records.** `public record XxxRequest(@NotBlank String name, ...) {}`.
2. **Validación en el borde.** El controlador usa `@Valid @RequestBody` y
   `@Validated` a nivel de clase para parámetros de ruta/consulta.
3. **Estados y tipos como `String`.** El DTO recibe `String` (p. ej. `type`,
   `status`, `action`) y el mapper REST llama a `fromCode(...)`. Código
   inválido ⇒ `InvalidDataException` ⇒ 400.
4. **Dinero.** Los requests reciben `BigDecimal amount` (nunca `double`), se
   convierten a `Money.of(amount)`; escala 2 `HALF_UP`. El dominio valida
   positividad.
5. **Fechas.** `LocalDate` en formato ISO (`yyyy-MM-dd`) para reportes; se
   valida que `from <= to` en el servicio (`InvalidDateRangeException`).
6. **Sin entidades de dominio en requests.** Solo tipos primitivos/VOs
   serializables; el mapper REST construye los VOs.
7. **Respuestas sin datos sensibles.** `UserResponse` nunca incluye
   `passwordHash`; `AuthResponse` incluye solo `token` + `UserResponse`.
8. **Campos desconocidos.** JSON con propiedades extra se rechaza o ignora según
   configuración; no se permite sobrescribir IDs en el body (el ID va en la
   ruta).

## 2. DTOs de request y anotaciones

| DTO | Campos principales | Validación |
|---|---|---|
| `RegisterBuyerRequest` | `fullName`, `email`, `identityDocument`, `password`, `mainAddress` | `@NotBlank`, `@Email`, `@Size(min=5,max=30)`, `@Valid` dirección |
| `LoginRequest` | `email`, `password` | `@NotBlank`, `@Email` |
| `AddCartItemRequest` | `productId`, `quantity` | `@NotBlank`, `@Positive` |
| `UpdateCartItemRequest` | `quantity` | `@Positive` |
| `CheckoutRequest` | `deliveryAddress` | `@NotNull @Valid` |
| `RequestReturnRequest` | `reason` | `@NotBlank @Size(max=1000)` |
| `AddAddressRequest` / `ChangeMainAddressRequest` | `address` | `@NotNull @Valid` |
| `CreateProductRequest` | `name`, `description`, `type`, `price` | `@NotBlank`, `@NotNull`, `@DecimalMin("0.01")`, `type` `@Pattern` válido |
| `UpdateProductRequest` | `name`, `description`, `price` | `@NotBlank`, `@DecimalMin("0.01")` |
| `AddVariantRequest` | `type`, `value` | `@NotBlank`, `@Size(max=50)` |
| `RegisterInventoryRequest` | `productId`, `warehouseId`, `initialQuantity` | `@NotBlank`, `@PositiveOrZero` |
| `CreateShipmentRequest` | `orderId`, `warehouseId` | `@NotBlank` |
| `StartShipmentRequest` | `trackingNumber` | `@NotBlank @Size(max=100)` |
| `InventoryAdjustmentRequest` | `quantity` (inflow) / `delta` (adjust) | `@Positive` / `@NotNull` (delta ≠ 0 se valida en dominio) |
| `RegisterSellerRequest` | `userId`, `taxId` | `@NotBlank` |
| `UpdateSellerRequest` | `taxId` | `@NotBlank` |
| `CreateWarehouseRequest` | `name`, `location`, `type`, `ownerSellerId`, `responsibleUserId` | `@NotBlank`, `@NotNull`, `@Valid` dirección |
| `UpdateWarehouseRequest` | `name`, `location`, `responsibleUserId` | `@NotBlank`, `@NotNull`, `@Valid` |
| `CreateUserRequest` | `fullName`, `email`, `identityDocument`, `role`, `password` | `@NotBlank`, `@Email`, `@Size`, `@NotBlank` |
| `GenerateRefundRequest` | `amount` | `@NotNull @DecimalMin("0.01")` |

`AddressRequest` (anidado): `street`, `city`, `region`, `postalCode`, `country`
con `@NotBlank` salvo `postalCode` (opcional).

## 3. Conversión de catálogos

```java
ProductType type = ProductType.fromCode(request.type());
OrderStatus status = OrderStatus.fromCode(request.status());
```

- `null`/vacío o desconocido ⇒ `InvalidDataException` ⇒ 400.
- No se aceptan códigos de catálogo en minúsculas como valores de dominio; el
  `fromCode` normaliza pero el DTO conserva el texto original para el mensaje.

## 4. Mappers REST

Un mapper por agregado en `adapters.rest.mappers`:

`UserRestMapper`, `BuyerRestMapper`, `SellerRestMapper`,
`WarehouseRestMapper`, `ProductRestMapper`, `InventoryRestMapper`,
`CartRestMapper`, `OrderRestMapper`, `InvoiceRestMapper`,
`ShipmentRestMapper`, `ReturnRestMapper`, `RefundRestMapper`,
`ReportRestMapper`, `AuditLogRestMapper`.

Contrato:

```java
DomainType toDomain(RequestDto dto);
ResponseDto toResponse(DomainType domain);
```

- `toResponse` aplana Value Objects en objetos anidados (`MoneyResponse`,
  `AddressResponse`), nunca expone referencias cíclicas (`Seller.warehouses` ↔
  `Warehouse.owner`) más allá de IDs.
- Los importes se serializan como `{ "amount": 100.00, "currency": "COP" }`.

## 5. Errores de validación

`MethodArgumentNotValidException` y `ConstraintViolationException` se traducen
en `GlobalExceptionHandler` a HTTP 400 con:

```json
{
  "timestamp": "2026-11-01T15:30:00Z",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "quantity must be greater than 0",
  "path": "/api/v1/buyer/cart/items",
  "requestId": "..."
}
```

Se listan los campos inválidos en `message` (sin trazas ni detalles internos).

## 6. Pruebas previstas (Fase 5/7)

- `*RestMapperTest` (ida y vuelta, campos sensibles ausentes).
- `@WebMvcTest`/`MockMvc` standalone por controlador, con puertos de entrada
  mockeados: 200/201/204, 400 por validación, 401/403 de seguridad, 404/409 de
  dominio.
