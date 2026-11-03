# NexusMarket — Estado de implementación verificado

> Este documento refleja **solo lo comprobado** en el sistema de archivos y en Git.
> No es fuente de verdad del diseño: el SDD lo es desde la Fase 1.
> Última actualización: Fase 5 cerrada (rama `develop`).

## Entorno verificado

| Elemento | Estado |
|---|---|
| Java | Temurin 17.0.20.1 (`C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot`) |
| Maven | 3.9.16 vía Maven Wrapper (`distributionType=only-script`) |
| Spring Boot (parent) | 4.1.1 — resuelve correctamente |
| Rama de integración | `develop` |
| Remoto | `origin = https://github.com/mariavasquez66/MARKET-NEXUS.git` |

## Estado real del código

### Existe y compila

- `src/main/java/application/domain/enums/` — 18 catálogos `DomainCatalog`:
  `DomainCatalog`, `UserRole`, `UserStatus`, `CommercialStatus`, `ProductType`,
  `ProductStatus`, `OrderStatus`, `PaymentStatus`, `ShipmentStatus`, `ReturnStatus`,
  `InvoiceStatus`, `RefundStatus`, `WarehouseType`, `MovementType`,
  `InventoryCondition`, `VariantType`, `AuditAction`, `AuditEntityType`.
- `src/main/java/application/domain/exceptions/` — `DomainException` (abstracta,
  `RuntimeException`), `InvalidDataException` y 51 excepciones concretas agrupadas
  por contexto (`user`, `buyer`, `seller`, `warehouse`, `product`, `inventory`,
  `cart`, `order`, `invoice`, `logistics`, `returns`, `refund`).
- `src/main/java/application/domain/valueobjects/` — 9 VOs inmutables:
  `Money`, `Address`, `Email`, `FullName`, `IdentityDocument`, `TaxId`,
  `ProductVariant`, `PasswordHash`, `StockAllocation`.
- `src/main/java/application/domain/models/` — 16 agregados/entidades y 5 reportes:
  `User`, `Buyer`, `Seller`, `Warehouse`, `Product`, `Inventory`,
  `InventoryMovement`, `Cart`, `CartItem`, `Order`, `Invoice`, `Shipment`,
  `Return`, `Refund`, `AuditLog`, `AuthResult`; reportes `SalesReport`,
  `OrderSummary`, `InventorySummary`, `ReturnSummary`, `UserSummary`.
- `src/main/java/application/domain/ports/out/` — 18 puertos de salida.
- `src/main/java/application/domain/ports/in/` — 6 puertos de entrada por rol:
  `PublicAccessPort`, `NaturalCustomerPort`, `BusinessCustomerPort`,
  `BusinessOperatorPort`, `BusinessSupervisorPort`, `TellerEmployeePort`.
- `src/main/java/application/domain/services/` — 14 servicios de dominio y
  `ServiceSupport` (roles, auditoría). Desviaciones del SDD justificadas:
  `OrderService` recibe `InventoryMovementRepositoryPort`, `RefundService` recibe
  `OrderRepositoryPort`; `InventoryService.addStock`/`adjust` devuelven
  `InventoryMovement` para satisfacer `BusinessOperatorPort`.
- `src/main/java/application/adapters/useCases/` — 6 casos de uso que implementan
  los puertos de entrada y delegan 1:1 en los servicios.
- `src/main/java/application/adapters/persistence/jpa/` — entidades, conversores
  `AttributeConverter` por catálogo, repositorios Spring Data, mappers y adaptadores
  que implementan los 13 puertos JPA; estrategia de columnas planas FK, filas hijas
  y resolución sin ciclos.
- `src/main/java/application/adapters/persistence/mongodb/` — adaptador de auditoría
  (`AuditLogMongoAdapter`) implementando `AuditPort` con consultas por acción/rango
  de fechas.
- `src/main/java/application/adapters/rest/` — 22 DTOs de request con Jakarta
  Validation, 27 DTOs de response, 15 mappers REST y 6 controladores por rol:
  `PublicController`, `BuyerController`, `SellerController`,
  `LogisticsController`, `AdminController`, `SupervisorController`.
- `src/main/java/application/infrastructure/exception/` — `GlobalExceptionHandler`
  con `ErrorResponse` (`timestamp`, `status`, `code`, `message`, `path`,
  `requestId`) y mapeo de validación, catálogos inválidos, dominio y error 500.
- `src/test/java/application/domain/` — 124 pruebas: VOs, agregados y los 14
  servicios de dominio.
- `src/test/java/application/adapters/persistence/` — 69 pruebas de persistencia:
  round-trip de los 13 mappers JPA, los 17 converters de catálogo, restricciones
  de mapeo/unicidad, mapper y adaptador Mongo, y delegación de los 13 adaptadores
  JPA con repositorios mockeados.
- `src/test/java/application/adapters/rest/` y
  `src/test/java/application/infrastructure/` — 118 pruebas REST: round-trip de
  los 15 mappers, 7 pruebas del `GlobalExceptionHandler`, resolver de actor para
  MockMvc y 97 pruebas de controladores (200/201/204, 400, 401, 403, 404, 409).
- Total verificado: 311 pruebas (`124` dominio + `69` persistencia + `118` REST),
  0 fallos, 0 errores, 0 omitidas.
- Verificación de pureza: `application.domain` no importa Spring, JPA, Mongo,
  Jackson ni HTTP.

### No existe todavía

- Seguridad JWT, filtros, configuración de beans e integración (Fase 6).
- Pruebas de casos de uso y seguridad (Fases 6–7).

### SDD completo (Fase 1)

- `SDD/especificacion_funcional_nexusmarket.md` — OBJ-01…12, dominios,
  RG-01…03, glosario.
- `SDD/Decisiones-y-supuestos.md` — D-01…D-17, A-01…A-16 (D-09 ⚠).
- `SDD/Domain/Domain-Model.md`, `Enums-and-Value-Objects.md`,
  `Domain-Services.md`, `Exceptions.md`, `Input-ports.md`, `Output-ports.md`.
- `SDD/Domain/services/` — 14 contratos de servicio.
- `SDD/Adapters/Persistence-adapters.md`, `Use-cases-adapters.md`,
  `Api-rest-endpoints.md`, `Rest-validation.md`,
  `Global-exception-handler.md`, `Rest-security-cors.md`.
- `SDD/Orchestrator-prompt.md` — adaptado de la plantilla bancaria.
- `SDD/Traceability-Matrix.md` — requisito → artefacto → prueba.

### Verificaciones de la Fase 0

| Hallazgo | Resultado |
|---|---|
| H-01 código mínimo | Confirmado (10 enums + esqueleto vacío) |
| H-02 `PROGRESS.md` inflado | Confirmado; este documento lo sustituye |
| H-03 enums con BOM y sin `InvalidDataException` | Corregido; `compile` pasa |
| H-04 archivos vacíos | No se encontraron archivos de 0 bytes en `src/` |
| H-05 `SDD/` incompleta | Resuelto en Fase 1 (documentos de dominio y adaptadores) |
| H-06 inconsistencias entre fuentes | Resuelto documentalmente (D-03/D-04/D-06/D-10) |
| H-07 `DomainCatalog` sin `values()` | Resuelto: los 18 catálogos exponen `fromCode` y `values` |
| H-08 `pom.xml` incompleto | Metadatos corregidos; faltan validation/JWT (Fase 4) |
| H-09 PDF no legible | Transcrito como Anexo A en la especificación funcional |
| H-10 matriz de responsabilidades | Pendiente de confirmación (D-09) |

## Fases

| Fase | Estado | Evidencia |
|---|---|---|
| 0 — Diagnóstico y línea base | COMPLETA | `SDD/Diagnosis.md`, compilación OK |
| 1 — SDD completo | COMPLETA | `SDD/**`, `SDD/Traceability-Matrix.md` |
| 2 — Dominio core | COMPLETA | enums, excepciones, VOs, modelos, 18 puertos y 51 tests |
| 3 — Servicios y casos de uso | COMPLETA | 14 servicios, 6 puertos de entrada, 6 casos de uso y 124 tests |
| 4 — Persistencia JPA/Mongo | COMPLETA | JPA entidades/converters/repos/mappers/adaptadores, Mongo audit, 69 tests de persistencia y JJWT/validation en `pom.xml` |
| 5 — REST | COMPLETA | DTOs, mappers, 6 controladores, `GlobalExceptionHandler` y 118 pruebas REST; 311 pruebas totales |
| 6 — Seguridad e infraestructura | PENDIENTE | — |
| 7 — Cobertura y calidad | PENDIENTE | — |
| 8 — Cierre | PENDIENTE | — |

## Historial real de commits

```
26e1ff4 feat(rest): add supervisor endpoints
60093b2 feat(rest): add admin endpoints
70c2579 feat(rest): add seller and logistics endpoints
8d003ae feat(rest): add public and buyer endpoints
a2c5699 feat(rest): add DTOs, mappers and global exception handler
a41ef14 build(pom): add validation and JJWT dependencies
2e9c86a test(persistence): cover JPA adapters
f9ddc4e test(persistence): cover MongoDB audit adapter
73ef97a test(persistence): verify JPA mapping constraints
2d0b011 test(persistence): cover catalog converters
1c7336d test(persistence): cover JPA mapper round trips
f8923c1 chore(editor): enable interactive build configuration updates
4ca8f8a docs(progress): record persistence adapters
01217bd feat(persistence): add JPA entities, converters, repos, mappers and adapters
0a36306 docs(progress): close phase 3
7c38da6 feat(adapters): add use case adapters
683c343 feat(domain): expose seller-scoped service queries
ad35218 feat(domain): add input ports
267392f test(domain): cover domain service rules
8f9563f feat(domain): add domain services
cac4443 test(domain): cover value objects and aggregate invariants
2e00789 feat(domain): add output ports
20e6e1a feat(domain): add rich aggregates and report models
939917f feat(domain): add immutable value objects
d6ecd2d feat(domain): add exception hierarchy for all bounded contexts
9751f19 feat(domain): complete and align catalog enums
c1453c0 docs(sdd): add traceability matrix
5bcd1f2 docs(sdd): adapt orchestrator prompt to NexusMarket
ad365e7 docs(sdd): add adapter contracts
2f9d901 docs(sdd): add input and output ports
de0246f docs(sdd): add domain services and exceptions
b0476dd docs(sdd): add domain model and value objects
557120c docs(sdd): add design decisions and assumptions
3de2c17 docs(sdd): add functional specification in markdown
9269ad9 docs(sdd): add source specification and master prompt
6392639 docs(sdd): add repository diagnosis
e41b27a docs(progress): reset PROGRESS.md to verified state
1123956 chore(editor): save files as UTF-8 without BOM
f0c74b1 chore(git): ignore local env files and log output
df547ed build(pom): fill project metadata and remove empty sections
bb2d9f6 fix(domain): restore compiling baseline with BOM-free enums
b29f780 mvgit
81aa717 sdd
3353424 mvgit
5ddea94 arg
f40fdbe Initial commit
```

## Comandos de verificación

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd -q -DskipTests compile   # OK
.\mvnw.cmd -q test                  # 193 tests OK
```
