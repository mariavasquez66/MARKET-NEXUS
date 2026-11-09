# `LogisticsService`

**OBJ:** OBJ-10 · **Acceso:** `LOGISTIC_OPERATOR` (gestión), lectura autorizada
**Paquete:** `application.domain.services.LogisticsService`

## Dependencias (constructor)

`ShipmentRepositoryPort`, `OrderRepositoryPort`, `WarehouseRepositoryPort`,
`OrderService`, `AuditPort`, `ClockPort`.

## Métodos

### `Shipment createShipment(User actor, String orderId, String warehouseId)`

| Elemento | Detalle |
|---|---|
| Precondiciones | actor `LOGISTIC_OPERATOR`; pedido físico (`InvalidProductTypeException`); la bodega debe figurar en las asignaciones del pedido (`InvalidWarehouseTypeException`/`InvalidDataException`); no debe existir ya un envío para (pedido, bodega) (`DuplicateEntityException`) |
| Flujo | Carga pedido (`OrderNotFoundException`) y bodega (`WarehouseNotFoundException`); `Shipment.create(order, warehouse, order.deliveryAddress)`; guarda. |
| Auditoría | `SHIPMENT_CREATED` sobre `SHIPMENT` |

### `Shipment startShipping(User actor, String shipmentId, String trackingNumber)`

- Solo `LOGISTIC_OPERATOR`. `shipment.startShipping(trackingNumber)`; si ya
  inició ⇒ `ShipmentAlreadyStartedException`; tracking vacío ⇒
  `InvalidDataException`. Guarda.
- Tras iniciar, invoca `orderService.markDispatched(actor, orderId)`; el
  servicio de pedido solo cambia a `DISPATCHED` cuando **todas** las
  asignaciones tienen todos sus envíos en tránsito/entregado (D-08). La
  escritura del envío y la transición del pedido comparten la transacción del
  caso de uso.
- **Auditoría:** `SHIPMENT_STARTED` sobre `SHIPMENT`; `ORDER_STATUS_CHANGED` lo
  registra `OrderService` si aplica.

### `Shipment confirmDelivery(User actor, String shipmentId)`

- Solo `LOGISTIC_OPERATOR`. `shipment.confirmDelivery()` (`IN_TRANSIT` ⇒
  `DELIVERED`; si no ⇒ `InvalidStateTransitionException`). Guarda. Si todos los
  envíos del pedido están `DELIVERED`, invoca `orderService.markDelivered`.
  Ambas escrituras comparten la transacción del caso de uso; un fallo revierte
  el envío y el pedido.
- **Auditoría:** `SHIPMENT_DELIVERED` sobre `SHIPMENT`.

### `List<Shipment> listByOrder(User actor, String orderId)`

- `LOGISTIC_OPERATOR`, `SUPERVISOR`, `ADMIN` o el `BUYER` dueño del pedido.

### `Shipment getShipment(User actor, String shipmentId)`

- Mismas reglas de visibilidad que `listByOrder`.

## Reglas

- Un envío pertenece a un pedido y a una bodega de origen.
- Solo pedidos físicos generan envíos.
- El `trackingNumber` es obligatorio para iniciar el envío.
- El estado del pedido se recalcula automáticamente; no se expone una transición
  directa desde REST.
