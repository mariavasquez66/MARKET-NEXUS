# Excepciones de dominio

> Paquete raíz: `application.domain.exceptions`. Todas heredan de
> `DomainException` (abstracta, `RuntimeException`). Los nombres de clase llevan
> sufijo `Exception`; los mensajes están en inglés.
>
> `DomainException` e `InvalidDataException` ya existen desde la Fase 0.

## 1. Jerarquía

```
RuntimeException
└── DomainException (abstracta)
    ├── common
    │   ├── InvalidDataException
    │   ├── DuplicateEntityException
    │   ├── UnauthenticatedUserException
    │   ├── SingleRoleException
    │   ├── InsufficientPermissionException
    │   ├── UnauthorizedOperationException
    │   ├── InvalidDateRangeException
    │   └── NoDataException
    ├── user
    │   ├── UserNotFoundException
    │   ├── DuplicateEmailException
    │   ├── DuplicateIdentificationException
    │   ├── InactiveUserException
    │   └── InvalidRoleException
    ├── buyer
    │   ├── BuyerNotFoundException
    │   ├── RestrictedBuyerException
    │   └── InvalidAddressException
    ├── seller
    │   ├── SellerNotFoundException
    │   ├── SelfRegistrationNotAllowedException
    │   └── SellerWithoutWarehouseException
    ├── warehouse
    │   ├── WarehouseNotFoundException
    │   ├── InvalidWarehouseTypeException
    │   └── WarehouseAlreadyAssignedException
    ├── product
    │   ├── ProductNotFoundException
    │   ├── ProductNotPublishedException
    │   ├── InvalidPriceException
    │   ├── InvalidProductTypeException
    │   └── InvalidProductStateException
    ├── inventory
    │   ├── InventoryNotFoundException
    │   ├── InsufficientStockException
    │   ├── NegativeStockException
    │   ├── DamagedInventoryException
    │   └── InvalidReservationException
    ├── cart
    │   ├── CartNotFoundException
    │   ├── EmptyCartException
    │   ├── ProductNotAvailableException
    │   └── InvalidQuantityException
    ├── order
    │   ├── OrderNotFoundException
    │   ├── InvalidStateTransitionException
    │   ├── FinalizedOrderException
    │   └── CancelledOrderException
    ├── invoice
    │   ├── InvoiceNotFoundException
    │   ├── InvoiceAlreadyExistsException
    │   └── VoidedInvoiceException
    ├── logistics
    │   ├── ShipmentNotFoundException
    │   ├── ShipmentAlreadyStartedException
    │   └── InvalidDestinationAddressException
    ├── returns
    │   ├── ReturnNotFoundException
    │   ├── ReturnNotAllowedException
    │   └── ReturnAlreadyProcessedException
    └── refund
        ├── RefundNotFoundException
        ├── InvalidRefundAmountException
        └── RefundAlreadyProcessedException
```

## 2. Constructor y contrato

Cada excepción concreta expone:

```java
public class XxxException extends DomainException {
    public XxxException(String message) { super(message); }
    public XxxException(String message, Throwable cause) { super(message, cause); }
}
```

`DomainException` es `abstract` y no se lanza directamente.

## 3. Causa y uso por excepción

| Excepción | Se lanza cuando |
|---|---|
| `InvalidDataException` | dato de entrada nulo/vacío/formato inválido; base de validaciones |
| `DuplicateEntityException` | unicidad genérica (`taxId`, inventario producto-bodega, envío duplicado) |
| `UnauthenticatedUserException` | credenciales inválidas o falta de autenticación (RG-01) |
| `SingleRoleException` | intento de asignar/cambiar el rol de un usuario (RG-02) |
| `InsufficientPermissionException` | rol sin permiso para la operación (RG-03) |
| `UnauthorizedOperationException` | actor autenticado pero no dueño del recurso |
| `InvalidDateRangeException` | `from > to` en reportes o auditoría |
| `NoDataException` | reporte sin datos |
| `UserNotFoundException` | usuario inexistente |
| `DuplicateEmailException` / `DuplicateIdentificationException` | correo/documento ya registrado |
| `InactiveUserException` | actor `BLOCKED` o `INACTIVE` |
| `InvalidRoleException` | rol requerido distinto (p. ej. vendedor sin rol `SELLER`) |
| `BuyerNotFoundException` | comprador inexistente |
| `RestrictedBuyerException` | comprador `RESTRICTED`/`SUSPENDED` intenta comprar |
| `InvalidAddressException` | dirección inválida en operación de comprador/bodega/envío |
| `SellerNotFoundException` | vendedor inexistente |
| `SelfRegistrationNotAllowedException` | alta de vendedor por alguien que no es `ADMIN` (R-08) |
| `SellerWithoutWarehouseException` | producto físico sin bodega/inventario para publicar |
| `WarehouseNotFoundException` | bodega inexistente |
| `InvalidWarehouseTypeException` | tipo/dueño incoherente o bodega no apta |
| `WarehouseAlreadyAssignedException` | bodega ya asignada al vendedor |
| `ProductNotFoundException` | producto inexistente |
| `ProductNotPublishedException` | consulta pública de producto no publicado |
| `InvalidPriceException` | precio ≤ 0 |
| `InvalidProductTypeException` | operación incompatible con el tipo (digital con inventario/envío) |
| `InvalidProductStateException` | transición de producto inválida (p. ej. publicar descontinuado) |
| `InventoryNotFoundException` | inventario inexistente para producto/bodega |
| `InsufficientStockException` | reserva mayor que `availableQuantity` |
| `NegativeStockException` | ajuste que dejaría cantidad negativa |
| `DamagedInventoryException` | reserva/venta/devolución sobre inventario `DAMAGED` |
| `InvalidReservationException` | cantidad inválida o liberación/venta mayor que lo reservado |
| `CartNotFoundException` | carrito inexistente |
| `EmptyCartException` | checkout con carrito vacío |
| `ProductNotAvailableException` | producto no `PUBLISHED` al añadir/checkout |
| `InvalidQuantityException` | cantidad ≤ 0 o ajuste 0 |
| `OrderNotFoundException` | pedido inexistente |
| `InvalidStateTransitionException` | transición no permitida en la máquina de estados |
| `FinalizedOrderException` | modificación de pedido `DELIVERED`/`FINALIZED` (R-07) |
| `CancelledOrderException` | modificación de pedido `CANCELLED` (R-07) |
| `InvoiceNotFoundException` | factura inexistente |
| `InvoiceAlreadyExistsException` | segunda factura para un pedido |
| `VoidedInvoiceException` | segunda anulación de factura |
| `ShipmentNotFoundException` | envío inexistente |
| `ShipmentAlreadyStartedException` | iniciar un envío no `PENDING` |
| `InvalidDestinationAddressException` | destino de envío inválido |
| `ReturnNotFoundException` | devolución inexistente |
| `ReturnNotAllowedException` | devolución sobre pedido no entregado/finalizado o reembolso no permitido |
| `ReturnAlreadyProcessedException` | transición sobre devolución ya resuelta |
| `RefundNotFoundException` | reembolso inexistente |
| `InvalidRefundAmountException` | monto ≤ 0, > total o acumulado > total |
| `RefundAlreadyProcessedException` | segundo `process()` |

## 4. Código HTTP (contrato con `GlobalExceptionHandler`)

| HTTP | Excepciones |
|---|---|
| **400** | `InvalidDataException`, `InvalidDateRangeException`, errores de Jakarta Validation, `InvalidAddressException`, `InvalidPriceException`, `InvalidQuantityException`, `InvalidProductTypeException`, `InvalidDestinationAddressException`, `EmptyCartException` |
| **401** | `UnauthenticatedUserException`, JWT inválido/expirado/obsoleto |
| **403** | `InsufficientPermissionException`, `UnauthorizedOperationException`, `InactiveUserException`, `SelfRegistrationNotAllowedException`, `SingleRoleException`, `InvalidRoleException` |
| **404** | `*NotFoundException`, `NoDataException`, `ProductNotPublishedException`, `CartNotFoundException` |
| **409** | `Duplicate*`, `InvalidStateTransitionException`, `InvalidProductStateException`, `FinalizedOrderException`, `CancelledOrderException`, `InsufficientStockException`, `NegativeStockException`, `DamagedInventoryException`, `InvalidReservationException`, `WarehouseAlreadyAssignedException`, `SellerWithoutWarehouseException`, `InvoiceAlreadyExistsException`, `VoidedInvoiceException`, `ShipmentAlreadyStartedException`, `ReturnNotAllowedException`, `ReturnAlreadyProcessedException`, `RefundAlreadyProcessedException`, `InvalidRefundAmountException`, `ProductNotAvailableException` |
| **500** | Cualquier otra excepción no controlada (mensaje genérico, sin traza) |

## 5. Reglas

- Nunca se lanza `IllegalArgumentException`/`IllegalStateException` para una
  regla de negocio.
- Los mensajes no incluyen datos sensibles (hash, token, contraseña).
- `GlobalExceptionHandler` traduce la excepción a un cuerpo
  `{timestamp,status,code,message,path,requestId}`; ver
  `SDD/Adapters/Global-exception-handler.md`.
