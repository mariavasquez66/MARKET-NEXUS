# Autoevaluación — Rúbrica de 5 criterios

**Proyecto:** NexusMarket  
**Fecha de verificación:** 2026-11-03  
**Rama verificada:** `feat/spec-coverage`
**Nota final autoevaluada:** **5.0 / 5.0**

Fórmula aplicada:

```text
Nota = (5.0 × 0.20) + (5.0 × 0.30) + (5.0 × 0.25) + (5.0 × 0.15) + (5.0 × 0.10) = 5.0
```

La evaluación se basa únicamente en archivos y clases observables del repositorio. No se creó `evaluacion1.md`.

---

## 1. Especificación en SDD — 5.0 / 5.0 (20%)

**Estado:** Excelente.

**Evidencias:**

- `SDD/especificacion_funcional_nexusmarket.md`: OBJ-01 a OBJ-12, dominios funcionales, reglas RG-01 a RG-03 y glosario.
- `SDD/Decisiones-y-supuestos.md`: decisiones D-01 a D-17 y supuestos A-01 a A-16.
- `SDD/Domain/Domain-Model.md`: agregados, entidades, invariantes y relaciones.
- `SDD/Domain/Enums-and-Value-Objects.md`: catálogos y 9 Value Objects.
- `SDD/Domain/Domain-Services.md`: responsabilidades y reglas de los 14 servicios.
- `SDD/Domain/Input-ports.md` y `SDD/Domain/Output-ports.md`: puertos por rol y puertos de salida.
- `SDD/Domain/Exceptions.md`: jerarquía y mapeo HTTP.
- `SDD/Adapters/Api-rest-endpoints.md`, `Persistence-adapters.md`, `Use-cases-adapters.md`, `Rest-validation.md`, `Global-exception-handler.md` y `Rest-security-cors.md`: contratos de adaptadores.
- `SDD/Traceability-Matrix.md`: trazabilidad requisito → artefacto → prueba.
- `SDD/Diagnosis.md`: diagnóstico inicial y hallazgos H-01 a H-10.

**Hallazgos:** la documentación cubre el dominio completo, las reglas críticas, la arquitectura hexagonal, los contratos REST, la persistencia y las decisiones de diseño. Las desviaciones de implementación están registradas y justificadas.

---

## 2. Modelado de entidades en Java — 5.0 / 5.0 (30%)

**Estado:** Excelente.

**Evidencias:**

- `src/main/java/application/domain/models/User.java`: identidad, rol único, estado, bloqueo/activación, invalidación de tokens y `assertCanOperate`.
- `src/main/java/application/domain/models/Buyer.java`: direcciones, estado comercial y `assertCanPurchase`.
- `src/main/java/application/domain/models/Seller.java`: `registerBy(admin, ...)`, asignación de bodegas y validación de rol.
- `src/main/java/application/domain/models/Warehouse.java`: tipo, propietario, responsable y reglas de asignación.
- `src/main/java/application/domain/models/Product.java`: publicación, suspensión, descontinuación, variantes y validación de precio.
- `src/main/java/application/domain/models/Inventory.java`: stock disponible/reservado, reserva, venta, ajuste, daño y reparación.
- `src/main/java/application/domain/models/Cart.java` y `CartItem.java`: cantidades, duplicados y solo productos publicados.
- `src/main/java/application/domain/models/Order.java`: máquina de estados, asignaciones de stock, factura y bloqueo de pedidos finalizados.
- `src/main/java/application/domain/models/Invoice.java`, `Shipment.java`, `Return.java` y `Refund.java`: ciclo de facturación, logística y posventa.
- `src/main/java/application/domain/models/InventoryMovement.java` y `AuditLog.java`: registros inmutables.
- `src/main/java/application/domain/models/AuthResult.java` y `SalesReport`, `OrderSummary`, `InventorySummary`, `ReturnSummary`, `UserSummary`: modelos de resultado.
- `src/test/java/application/domain/models/DomainRichnessTest.java`: verifica que no existan setters públicos y que las colecciones se expongan como vistas inmutables.
- `src/test/java/application/domain/models/`: pruebas por agregado (`UserTest`, `BuyerTest`, `SellerTest`, `WarehouseTest`, `ProductTest`, `InventoryTest`, `CartTest`, `OrderTest`, `InvoiceTest`, `ShipmentTest`, `ReturnTest`, `RefundTest`).

**Hallazgos:** las entidades tienen identidad, constructores/factorías que validan invariantes y métodos de intención. No se usan `@Data` ni `@Setter` en `domain/models`; los cambios de estado ocurren mediante métodos de negocio.

---

## 3. Value Objects vs. primitivos — 5.0 / 5.0 (25%)

**Estado:** Excelente.

**Evidencias:**

- `src/main/java/application/domain/valueobjects/`: 9 Value Objects inmutables:
  - `Money`: `BigDecimal`, escala 2, `HALF_UP`, moneda COP y validación de no negatividad.
  - `Email`, `FullName`, `IdentityDocument`, `TaxId`, `PasswordHash`, `Address`, `ProductVariant` y `StockAllocation`.
- `src/main/java/application/domain/enums/`: 18 catálogos basados en `DomainCatalog`, entre ellos:
  - `UserRole`, `UserStatus`, `CommercialStatus`, `ProductType`, `ProductStatus`.
  - `OrderStatus`, `PaymentStatus`, `ShipmentStatus`, `ReturnStatus`, `InvoiceStatus`, `RefundStatus`.
  - `WarehouseType`, `MovementType`, `InventoryCondition`, `VariantType`, `AuditAction`, `AuditEntityType`.
- `src/main/java/application/domain/models/`: estados, roles, tipos, condiciones y movimientos se declaran con catálogos, no con `String`.
- `src/main/java/application/adapters/rest/mappers/`: conversión desde `String` de entrada mediante `fromCode(...)`.
- `src/test/java/application/domain/ValueObjectsTest.java`: invariantes de los Value Objects.
- `src/test/java/application/adapters/rest/RestMappersTest.java` y `src/test/java/application/adapters/persistence/CatalogConvertersTest.java`: round-trip de catálogos.

**Hallazgos:** no se detectaron `String` abiertos para campos acotados en `domain/models`. Los `String` restantes corresponden a texto libre real, como nombres, descripciones, motivos y números de seguimiento. El dinero usa `Money` con `BigDecimal`; no se usan `double` ni `float` monetarios.

---

## 4. Idioma y convenciones Java — 5.0 / 5.0 (15%)

**Estado:** Excelente.

**Evidencias:**

- Paquetes, clases, interfaces, métodos, atributos, constantes, excepciones y nombres de tests están en inglés.
- Los mensajes de excepción están en inglés y se construyen mediante las excepciones de dominio.
- Las claves JSON y los DTOs están en inglés: `AuthResponse`, `OrderResponse`, `InventoryResponse`, `ErrorResponse`.
- La documentación SDD y el `README.md` están en español, como exige el proyecto.
- Verificación de identificadores en español en `src/`: sin coincidencias en nombres de clases, métodos, atributos o paquetes.

**Hallazgos:** no se detectó spanglish en identificadores Java. Las únicas apariciones de términos en español corresponden a documentación, mensajes de commit o textos de negocio, no a nombres de código.

---

## 5. Correlación SDD vs. código Java — 5.0 / 5.0 (10%)

**Estado:** Excelente.

**Evidencias:**

- `SDD/Traceability-Matrix.md` relaciona cada objetivo, agregado, catálogo, Value Object, servicio, puerto, adaptador, excepción y endpoint con su artefacto de código y su prueba.
- Los 21 modelos documentados existen en `src/main/java/application/domain/models/`.
- Los 18 catálogos documentados existen en `src/main/java/application/domain/enums/`.
- Los 9 Value Objects documentados existen en `src/main/java/application/domain/valueobjects/`.
- Los 14 servicios documentados existen en `src/main/java/application/domain/services/`.
- Los 6 puertos de entrada y 18 puertos de salida documentados existen en `src/main/java/application/domain/ports/`.
- Los 6 controladores documentados existen en `src/main/java/application/adapters/rest/controllers/`.
- Los 13 adaptadores JPA, el adaptador Mongo y los adaptadores de seguridad documentados existen en `src/main/java/application/adapters/` e `src/main/java/application/infrastructure/`.
- Las diferencias justificadas están registradas en `PROGRESS.md` y `SDD/Decisiones-y-supuestos.md`:
  - `OrderService` recibe también `InventoryMovementRepositoryPort`.
  - `RefundService` recibe `OrderRepositoryPort`.
  - `InventoryService.addStock` y `adjust` devuelven `InventoryMovement` para cumplir el puerto de operador logístico.

**Hallazgos:** la correspondencia es 1:1 en nombres y responsabilidades. No se encontraron clases documentadas sin implementación ni clases de dominio sin trazabilidad documental.

---

## Verificaciones ejecutadas

| Verificación | Resultado |
|---|---|
| `.\mvnw.cmd clean test` | 364 pruebas, 0 fallos, 0 errores, 0 omitidas |
| Imports de frameworks en `application.domain` | Sin coincidencias |
| `String` abiertos para campos acotados en `domain/models` | Sin hallazgos |
| `Double`/`double`/`float` monetarios | Sin hallazgos |
| Identificadores en español en `src/` | Sin hallazgos |
| Elementos de la matriz sin clase existente | Sin hallazgos |
| `.env` versionado | No; solo `.env.example` |
| `evaluacion1.md` en el repositorio | No existe |

## Observaciones y riesgos residuales

- D-09 quedó **confirmada** contra la matriz de PDF p.8 §12; las 4 columnas existen. Solo las filas/roles ausentes permanecen como supuesto.
- `SDD/Gap-Analysis.md` registra que el PDF no respalda 28 servicios/90 endpoints; no se añadieron servicios vacíos ni endpoints artificiales.
- Las pruebas son unitarias y de contexto Spring con dependencias mockeadas; no se ejecutan pruebas de integración con contenedores. La conectividad con MySQL y MongoDB se verificó manualmente.
- MongoDB 4.4 está fijado por limitación de AVX del host de verificación (D-17); en un host compatible puede actualizarse la imagen.
