# Gap Analysis — PDF funcional vs. implementación

> Base: `SDD/Spec-Ledger/ledger.md` y pasadas 1–4. El PDF tiene 8 páginas y 12 objetivos; no contiene detalle de diseño técnico.

## 1. Cobertura funcional

| Área PDF | Requisitos | Servicio(s) actuales | Estado |
|---|---|---|---|
| Usuarios | REQ-002, 028–032, 053–056 | `UserService`, `AuthService` | Cubierto; catálogo de estados incompleto en PDF |
| Compradores | REQ-004, 033–036 | `BuyerService`, `AuthService` | Cubierto |
| Vendedores | REQ-003, 037, 057 | `SellerService` | Cubierto |
| Bodegas | REQ-005, 038–039 | `WarehouseService` | Cubierto |
| Catálogo | REQ-006, 040–044, 058 | `CatalogService` | Cubierto |
| Inventario | REQ-007, 045–049, 059, 076 | `InventoryService` | Cubierto con brechas de autorización/transacción |
| Carrito | REQ-008, 024 | `CartService` | Cubierto |
| Pedidos | REQ-009, 024–027, 050–052, 060 | `OrderService` | Cubierto con brechas de transición/transacción |
| Facturación | REQ-010, 062 | `InvoiceService` | Cubierto; reglas fiscales son supuesto |
| Logística | REQ-011, 026, 063 | `LogisticsService` | Cubierto con brechas de transición/transacción |
| Devoluciones/reembolsos | REQ-012, 064–065, 073 | `ReturnService`, `RefundService` | Parcial; reglas no definidas en PDF |
| Reportes | REQ-013, 066, 072 | `ReportService` | Parcial; contenido y rol no definidos en PDF |

## 2. Decisión sobre la ampliación a 28 servicios / 90 endpoints

**No se amplía artificialmente.** El PDF solo define 7 dominios funcionales, 12 objetivos y 13 procesos. Los 14 servicios y 64 endpoints actuales ya cubren todos los objetivos y procesos verificables. Llegar a 28 servicios y 90 endpoints exigiría inventar áreas, duplicar operaciones o exponer detalles internos sin respaldo funcional, lo que contradice RVI y la instrucción de no inflar con servicios vacíos.

| Meta | ¿Respaldo PDF? | Decisión |
|---|---|---|
| ≥28 servicios | No | Mantener 14 servicios cohesionados; dividir solo si una operación demuestra cohesión insuficiente, sin crear servicios vacíos. |
| ≥90 endpoints | No | Mantener 64 endpoints; añadir solo los necesarios para corregir brechas verificadas. |
| Metas 96–110 | No | No aplican. |

Los supuestos de IVA, cancelación, envíos por bodega, estados comerciales, reportes y aprobación de devoluciones quedan en `SDD/Supuestos-no-verificados.md`; no justifican expansión.

## 3. Defectos técnicos verificados (F3)

| ID | Defecto | Evidencia | Corrección prevista | Prueba |
|---|---|---|---|---|
| DEF-01 | Los casos de uso no abren transacción; las escrituras múltiples pueden quedar parcialmente confirmadas. | `src/main/java/application/adapters/useCases/*.java` no tienen `@Transactional`. | Anotar las 6 clases con `@Transactional` y marcar lecturas puras como `readOnly`. | Prueba de arquitectura/reflexión sobre anotaciones. |
| DEF-02 | `AuthService.registerBuyer` guarda usuario y comprador por separado; si falla el segundo, queda un usuario huérfano. | `AuthService.java:70-73`. | Cubrir con transacción del caso de uso y probar fallo del segundo guardado. | Test de caso de uso con repositorio que falla. |
| DEF-03 | `OrderService.checkout` no libera reservas si falla después de reservar y vuelve a guardar el inventario todavía reservado. | `OrderService.java:83-110`. | Liberar cada reserva y guardar; luego relanzar. | Test con fallo posterior a la reserva. |
| DEF-04 | `LogisticsService.startShipping` guarda el envío y luego cambia el pedido; si falla el pedido, el envío queda inconsistente. | `LogisticsService.java:73-84`. | Transacción de caso de uso + prueba de fallo en `markDispatched`. | Test de caso de uso transaccional. |
| DEF-05 | `ReturnService.complete` cambia la devolución y luego registra inventario; un fallo intermedio deja estado parcial. | `ReturnService.java:75-88`. | Transacción de caso de uso + orden de escritura seguro. | Test de fallo en registro de inventario. |
| DEF-06 | `OrderService.markDispatched` acepta “algún” envío en tránsito por bodega, no todos. | `OrderService.java:166-175`. | Exigir lista no vacía y `allMatch(IN_TRANSIT o DELIVERED)`. | Test con un envío pendiente y otro en tránsito. |
| DEF-07 | `OrderService.markDelivered` acepta “algún” envío entregado por bodega, no todos. | `OrderService.java:189-197`. | Exigir lista no vacía y `allMatch(DELIVERED)`. | Test con un envío entregado y otro en tránsito. |
| DEF-08 | `InventoryService.reserve/releaseReservation/confirmSale` permiten mutar stock fuera del flujo de pedido, sin idempotencia ni vínculo a una orden. | `InventoryService.java:92-115`. | Restringir a operaciones internas del flujo de pedidos o eliminar exposición no usada. | Test de API interna/roles. |
| DEF-09 | Los mocks de repositorios devuelven la misma instancia mutable y no simulan recargas; ocultan los defectos DEF-02…DEF-07. | `OrderServiceTest.java:70-74`, `ReturnServiceTest`, `LogisticsServiceTest`. | Fakes que copian en `save` y devuelven copias en consultas; pruebas de fallo. | Suite de fakes + casos de fallo. |

## 4. Brechas no cubiertas por el PDF

| Brecha | Estado | Acción |
|---|---|---|
| IVA 19% y precios con IVA incluido | Implementado por D-12 | Supuesto; no se amplía |
| Cancelación antes del pago | Implementado | Supuesto; no se amplía |
| Un envío por bodega | Implementado por convención | Supuesto; no se amplía |
| Aprobación de devoluciones | Admin | Supuesto; no se amplía |
| Reportes solo Supervisor | Implementado | Supuesto; no se amplía |
| Catálogo completo de estados de usuario/comerciales | Parcial | Supuesto; no se amplía |

## 5. Conclusión

La implementación cubre el PDF. El trabajo pendiente es de **integridad transaccional, autorización y consistencia**, no de cantidad de servicios/endpoints. F4 y F5 quedan **N/A por falta de respaldo funcional**, salvo que la persona dueña confirme requisitos adicionales.
