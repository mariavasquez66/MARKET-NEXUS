# `ReportService`

**OBJ:** OBJ-12 · **Acceso:** `SUPERVISOR` (solo lectura)
**Paquete:** `application.domain.services.ReportService`

## Dependencias (constructor)

`OrderRepositoryPort`, `InventoryRepositoryPort`, `ReturnRepositoryPort`,
`UserRepositoryPort`, `AuditPort`, `ClockPort`.

## Métodos

### `SalesReport salesReport(User actor, LocalDate from, LocalDate to)`

| Elemento | Detalle |
|---|---|
| Precondiciones | actor `SUPERVISOR`; `from` y `to` no nulos |
| Flujo | 1) Si `from.isAfter(to)` ⇒ `InvalidDateRangeException`. 2) Obtiene pedidos con `paymentStatus == CONFIRMED` y `createdAt` dentro del rango. 3) Si no hay pedidos ⇒ `NoDataException`. 4) Suma `totalAmount` y cuenta pedidos. 5) Devuelve `SalesReport(from, to, orderCount, totalSales)`. |
| Excepciones | `InvalidDateRangeException`, `NoDataException` |

### `OrderSummary orderSummary(User actor)`

- `SUPERVISOR`. Cuenta pedidos totales y por cada `OrderStatus`; si no hay
  pedidos ⇒ `NoDataException`. Devuelve `OrderSummary`.

### `InventorySummary inventorySummary(User actor)`

- `SUPERVISOR`. Suma registros, disponibles, reservadas y registros `DAMAGED`;
  si no hay inventario ⇒ `NoDataException`. Devuelve `InventorySummary`.

### `ReturnSummary returnSummary(User actor)`

- `SUPERVISOR`. Cuenta devoluciones por `ReturnStatus`; si no hay ⇒
  `NoDataException`. Devuelve `ReturnSummary`.

### `UserSummary userSummary(User actor)`

- `SUPERVISOR`. Cuenta usuarios por `UserRole`; si no hay ⇒ `NoDataException`.
  Devuelve `UserSummary`.

### `List<AuditLog> auditLogs(User actor, AuditAction action, LocalDate from, LocalDate to)`

- `SUPERVISOR`. Filtros opcionales (`action` puede ser nulo). Si `from > to` ⇒
  `InvalidDateRangeException`. Delegado a `AuditPort.find(action, from, to)`.

## Reglas

- Ningún método de reporte modifica datos.
- Todos los importes se agregan con `Money` (sin `double`).
- Un rango invertido siempre es `InvalidDateRangeException`.
- Un reporte sin datos es `NoDataException` (HTTP 404).
