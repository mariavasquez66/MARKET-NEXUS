# Global Exception Handler

> Clase: `application.infrastructure.exception.GlobalExceptionHandler`
> (o `adapters.rest.controllers.GlobalExceptionHandler` si se prefiere dentro de
> REST; se implementará en `infrastructure/exception`).
> Anotación: `@RestControllerAdvice`.

## 1. Cuerpo de error

```json
{
  "timestamp": "2026-11-01T15:30:00Z",
  "status": 409,
  "code": "INVALID_STATE_TRANSITION",
  "message": "Order cannot transition from FINALIZED to CANCELLED",
  "path": "/api/v1/buyer/orders/abc/cancel",
  "requestId": "9f1c..."
}
```

| Campo | Origen |
|---|---|
| `timestamp` | `ClockPort.now()`/`Instant.now()` en el handler |
| `status` | HTTP numérico |
| `code` | código estable en `UPPER_SNAKE_CASE` |
| `message` | mensaje de la excepción (en inglés) o genérico para 500 |
| `path` | `HttpServletRequest.getRequestURI()` |
| `requestId` | cabecera `X-Request-Id` inyectada por el filtro |

Nunca se incluye stack trace, SQL, nombre de clase interna ni datos sensibles.

## 2. Tabla de mapeo

| Excepción | HTTP | `code` |
|---|---|---|
| `MethodArgumentNotValidException` | 400 | `VALIDATION_ERROR` |
| `ConstraintViolationException` | 400 | `VALIDATION_ERROR` |
| `HttpMessageNotReadableException` | 400 | `MALFORMED_REQUEST` |
| `InvalidDataException` | 400 | `INVALID_DATA` |
| `InvalidDateRangeException` | 400 | `INVALID_DATE_RANGE` |
| `InvalidAddressException` | 400 | `INVALID_ADDRESS` |
| `InvalidPriceException` | 400 | `INVALID_PRICE` |
| `InvalidQuantityException` | 400 | `INVALID_QUANTITY` |
| `InvalidProductTypeException` | 400 | `INVALID_PRODUCT_TYPE` |
| `InvalidDestinationAddressException` | 400 | `INVALID_DESTINATION_ADDRESS` |
| `EmptyCartException` | 400 | `EMPTY_CART` |
| `UnauthenticatedUserException` | 401 | `UNAUTHENTICATED` |
| JWT inválido/expirado/obsoleto (`JwtException`, `SecurityException`) | 401 | `INVALID_TOKEN` |
| `InsufficientPermissionException` | 403 | `FORBIDDEN` |
| `UnauthorizedOperationException` | 403 | `UNAUTHORIZED_OPERATION` |
| `InactiveUserException` | 403 | `INACTIVE_USER` |
| `SelfRegistrationNotAllowedException` | 403 | `SELF_REGISTRATION_NOT_ALLOWED` |
| `SingleRoleException` | 403 | `SINGLE_ROLE_VIOLATION` |
| `InvalidRoleException` | 403 | `INVALID_ROLE` |
| `*NotFoundException` | 404 | `NOT_FOUND` |
| `NoDataException` | 404 | `NO_DATA` |
| `ProductNotPublishedException` | 404 | `PRODUCT_NOT_PUBLISHED` |
| `DuplicateEntityException`, `DuplicateEmailException`, `DuplicateIdentificationException` | 409 | `DUPLICATE_RESOURCE` |
| `InvalidStateTransitionException` | 409 | `INVALID_STATE_TRANSITION` |
| `InvalidProductStateException` | 409 | `INVALID_PRODUCT_STATE` |
| `FinalizedOrderException` | 409 | `FINALIZED_ORDER` |
| `CancelledOrderException` | 409 | `CANCELLED_ORDER` |
| `InsufficientStockException` | 409 | `INSUFFICIENT_STOCK` |
| `NegativeStockException` | 409 | `NEGATIVE_STOCK` |
| `DamagedInventoryException` | 409 | `DAMAGED_INVENTORY` |
| `InvalidReservationException` | 409 | `INVALID_RESERVATION` |
| `WarehouseAlreadyAssignedException` | 409 | `WAREHOUSE_ALREADY_ASSIGNED` |
| `SellerWithoutWarehouseException` | 409 | `SELLER_WITHOUT_WAREHOUSE` |
| `InvoiceAlreadyExistsException` | 409 | `INVOICE_ALREADY_EXISTS` |
| `VoidedInvoiceException` | 409 | `VOIDED_INVOICE` |
| `ShipmentAlreadyStartedException` | 409 | `SHIPMENT_ALREADY_STARTED` |
| `ReturnNotAllowedException` | 409 | `RETURN_NOT_ALLOWED` |
| `ReturnAlreadyProcessedException` | 409 | `RETURN_ALREADY_PROCESSED` |
| `RefundAlreadyProcessedException` | 409 | `REFUND_ALREADY_PROCESSED` |
| `InvalidRefundAmountException` | 409 | `INVALID_REFUND_AMOUNT` |
| `ProductNotAvailableException` | 409 | `PRODUCT_NOT_AVAILABLE` |
| Cualquier otra `Exception` | 500 | `INTERNAL_ERROR` |

## 3. Implementación

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidDataException.class)
    public ResponseEntity<ErrorResponse> handleInvalidData(
            InvalidDataException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "INVALID_DATA", ex.getMessage(), request);
    }

    // ... un @ExceptionHandler por familia o un mapa de excepción → (status, code)

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(
            Exception ex, HttpServletRequest request) {
        log.error("Unexpected error", ex); // log interno; la respuesta es genérica
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "An unexpected error occurred", request);
    }
}
```

- La clasificación de excepciones de dominio se hace por tipo (`instanceof`),
  nunca comparando el nombre de la clase.
- `requestId` se obtiene de `request.getAttribute("requestId")` (lo fija el
  filtro `X-Request-Id`); si falta, se genera uno.
- Se puede usar un `Map<Class<? extends DomainException>, HttpStatus>` para no
  repetir métodos; el contrato HTTP de `SDD/Domain/Exceptions.md` es la fuente.
- Los errores de seguridad (401/403) también pasan por este handler mediante
  `AuthenticationEntryPoint` y `AccessDeniedHandler` personalizados.
- `ErrorResponse` es un `record` en `adapters.rest.dto.response`.

## 4. Pruebas previstas

- `GlobalExceptionHandlerTest` con `MockMvc` standalone: una excepción de cada
  familia (400/401/403/404/409/500), verificando `code`, `message`, `path` y
  `requestId`.
- Verificar que un 500 no expone la traza ni el mensaje interno.
