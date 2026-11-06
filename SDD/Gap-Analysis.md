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

**Reevaluación F4/F5 (máximo defendible, sin requisitos nuevos).** El PDF solo define 7 dominios funcionales, 12 objetivos y 13 procesos. No respalda 28 servicios ni 90 endpoints, pero sí permite separar responsabilidades internas y completar lecturas necesarias para procesos ya especificados. Se pasa de 14 a 22 clases de servicio (14 beans fachada), sin servicios vacíos ni reglas nuevas, y de 64 a 72 endpoints trazables a `[SPEC]`/`[DERIVADO]`.

| Meta | ¿Respaldo PDF? | Decisión |
|---|---|---|
| ≥28 servicios | No | 22 clases por responsabilidad: 8 divisiones internas (`Checkout`, pago, cumplimiento, ciclo de vida y consulta de pedido; consulta de inventario y catálogo; elegibilidad de comprador). No se llega a 28. |
| ≥90 endpoints | No | 72 endpoints: los 8 nuevos cubren lectura de direcciones, finalización de pedido, producto propio, envíos y consultas admin; no se inventan rutas. |
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
| DEF-08 | La autorización de `listBySeller`/`listByProduct` valida solo el primer producto o ninguno; un vendedor puede leer inventario ajeno. | `InventoryService.java`, métodos `listBySeller` y `listByProduct`. | Validar que todos los inventarios devueltos pertenezcan al vendedor. | Tests de fuga con vendedor sin productos y con producto ajeno. |
| DEF-09 | La suite no verificaba límites transaccionales ni el rollback/compensación de escrituras multiagregado. | Tests de servicios no cubrían fallo posterior a reserva ni anotaciones de casos de uso. | Añadir prueba de anotaciones y prueba de fallo al guardar pedido. | `UseCaseTransactionTest` y `checkoutReleasesReservationsWhenOrderSaveFails`. |

Estado F3: **DEF-01…DEF-09 corregidos** en `28e6ee2`; cubiertos por
`UseCaseTransactionTest`, `OrderServiceTest` e `InventoryServiceTest`. La suite
completa queda en 393 pruebas verdes.

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

La implementación cubre el PDF. F4 y F5 se ejecutaron hasta donde el texto permite: 22 clases de servicio por responsabilidad y 72 endpoints con trazabilidad RVI. No se alcanzan 28/90 porque el PDF no los respalda; ampliar más exigiría requisitos nuevos. El trabajo pendiente sigue siendo de **integridad transaccional, autorización y consistencia**.
