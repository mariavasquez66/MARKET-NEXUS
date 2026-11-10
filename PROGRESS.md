# NexusMarket — Estado de implementación verificado

> Este documento refleja **solo lo comprobado** en el sistema de archivos y en Git.
> No es fuente de verdad del diseño: el SDD lo es desde la Fase 1.
> Última actualización: misión docente en `fix/demo-docente`, 429 pruebas en verde.

## Entorno verificado

| Elemento | Estado |
|---|---|
| Java | Temurin 17.0.20.1 (`C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot`) |
| Maven | 3.9.16 vía Maven Wrapper (`distributionType=only-script`) |
| Spring Boot (parent) | 4.1.1 — resuelve correctamente |
| Rama de integración | `develop` |
| Remoto | `origin = https://github.com/mariavasquez66/MARKET-NEXUS.git` |
| Docker | MySQL 8.0 y MongoDB 4.4 en contenedores saludables (puertos 3306/27017) |
| Arranque local | `Started NexusmarketApplication` y `GET /api/v1/public/auth/login` OK con admin bootstrap |

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
- `src/main/java/application/domain/services/` — 14 beans de dominio (22 clases:
  fachadas y servicios internos de pedido, inventario, catálogo y elegibilidad) y
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
- `src/main/java/application/NexusmarketApplication.java` — punto de entrada
  `@SpringBootApplication` (no existía en fases previas).
- `src/main/java/application/infrastructure/security/` — `JwtProvider`
  (claims `sub/jti/ver/iat/exp/role`, sin PII), `JwtAuthenticationFilter`
  (valida firma, expiración, usuario activo, versión de token y rol actual),
  `AuthenticatedUserPrincipal`, `SecurityConfig` (stateless, reglas por prefijo,
  `SUPERVISOR` solo `GET`, CORS, BCrypt), `RequestIdFilter`,
  `SecurityErrorWriter`, entry point 401 y access denied 403 en JSON.
- `src/main/java/application/infrastructure/config/` — `DomainServicesConfig`
  (14 servicios), `BootstrapAdminRunner` (D-16, `@Profile("!test")`) y
  `TaxPolicyAdapter` (`nexusmarket.tax.rate`).
- `src/main/resources/application*.properties` — perfiles `local` y `docker`;
  `docker-compose.yml` (MySQL 8.0 + MongoDB 4.4 con credenciales por entorno) y
  `.env.example`. MongoDB se fija en 4.4 (D-17) porque el host de verificación
  no soporta AVX y las imágenes 5.0+ terminan con exit code 132.
- `src/test/java/application/domain/` — 133 pruebas: VOs, agregados, validaciones
  críticas, inmutabilidad de colecciones y los servicios de dominio (fachadas e
  internos).
- `src/test/java/application/adapters/persistence/` — 71 pruebas de persistencia:
  round-trip de los 13 mappers JPA, los 17 converters de catálogo, restricciones
  de mapeo/unicidad, mapper y adaptador Mongo, y delegación de los 13 adaptadores
  JPA con repositorios mockeados.
- `src/test/java/application/adapters/rest/` y
  `src/test/java/application/infrastructure/` — 118 pruebas REST: round-trip de
  los 15 mappers, 7 pruebas del `GlobalExceptionHandler`, resolver de actor para
  MockMvc y 97 pruebas de controladores (200/201/204, 400, 401, 403, 404, 409).
- `src/test/java/application/infrastructure/security/` y
  `src/test/java/application/infrastructure/config/` — 37 pruebas:
  JWT (claims, expiración, firma, secreto inválido), filtro (401 por token
  inválido/obsoleto/rol alterado/usuario inactivo), reglas por prefijo y
  preflight CORS, `X-Request-Id`, BCrypt, bootstrap de administrador y tasa de
  impuestos.
- Total verificado: 359 pruebas (`133` dominio + `71` persistencia + `118` REST
  + `37` seguridad/configuración), 0 fallos, 0 errores, 0 omitidas.
- Verificación de pureza: `application.domain` no importa Spring, JPA, Mongo,
  Jackson ni HTTP.
- Fase 7 agregó 6 pruebas de reglas críticas: inmutabilidad de pedidos
  `DELIVERED`, transiciones inválidas, venta de inventario dañado, cantidades
  negativas en reconstitución y ausencia de setters/colecciones mutables en
  entidades ricas.

### Pendientes no bloqueantes

- No hay pruebas dedicadas a las clases de `adapters/useCases`; su delegación
  es trivial y está cubierta indirectamente por las pruebas de controladores y
  servicios.
- No se mide cobertura formal con JaCoCo.
- No hay pruebas automatizadas de integración con contenedores; la conectividad
  se verificó manualmente contra MySQL y MongoDB locales.

### Limitaciones de verificación

- La compilación y las 359 pruebas se ejecutaron con Java 17 y Maven Wrapper;
  la suite no necesita MySQL ni MongoDB.
- El arranque local se verificó con MySQL 8.0 y MongoDB 4.4; se confirmaron 18
  tablas en `nexusmarket_db`, la columna `condition_code` y el admin
  `admin@nexusmarket.local` con rol `ADMIN` y estado `ACTIVE`.
- El login de bootstrap devolvió token y usuario sin exponer credenciales.

### Evidencia de cierre de Fase 6

- Se registró un vendedor vía `POST /api/v1/admin/sellers`; el usuario se creó
  con rol `SELLER` y en `nexus_audit_db.audit_logs` apareció el evento
  `SELLER_REGISTERED` con `entityTypeCode: SELLER` y `roleCode: ADMIN`.
- Autorización verificada: un token `ADMIN` sobre una ruta `BUYER` devolvió
  `403`; una petición sin token devolvió `401`.
- Pureza verificada con
  `grep -RInE 'org\.springframework|jakarta\.persistence|com\.fasterxml' src/main/java/application/domain`
  sin coincidencias (exit code 1).
- Secretos: `.env` existe pero está ignorado; `git ls-files` solo incluye
  `.env.example`. No hay `arranque.log` versionado.

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
- `SDD/Self-check-rubric.md` — autoevaluación contra los 5 criterios, con
  rutas y clases como evidencia.
- `README.md` — descripción, arquitectura, variables de entorno, puesta en
  marcha, pruebas, roles y tabla de los 72 endpoints.

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
| H-08 `pom.xml` incompleto | Resuelto: metadatos, validation y JJWT incorporados en Fase 4 |
| H-09 PDF no legible | Transcrito como Anexo A en la especificación funcional |
| H-10 matriz de responsabilidades | Pendiente de confirmación (D-09) |

## Fases

| Fase | Estado | Evidencia |
|---|---|---|
| 0 — Diagnóstico y línea base | COMPLETA | `SDD/Diagnosis.md`, compilación OK |
| 1 — SDD completo | COMPLETA | `SDD/**`, `SDD/Traceability-Matrix.md` |
| 2 — Dominio core | COMPLETA | enums, excepciones, VOs, modelos, 18 puertos y pruebas de dominio |
| 3 — Servicios y casos de uso | COMPLETA | 14 beans (22 clases), 6 puertos de entrada y 6 casos de uso |
| 4 — Persistencia JPA/Mongo | COMPLETA | JPA entidades/converters/repos/mappers/adaptadores, Mongo audit, 71 pruebas de persistencia y JJWT/validation en `pom.xml` |
| 5 — REST | COMPLETA | DTOs, mappers, 6 controladores, `GlobalExceptionHandler` y 118 pruebas REST |
| 6 — Seguridad e infraestructura | COMPLETA | JWT/filtro, `SecurityFilterChain`, CORS + `X-Request-Id`, `BootstrapAdminRunner`, docker-compose, properties y 37 pruebas; 353 pruebas totales y arranque local verificado |
| 7 — Cobertura y calidad | COMPLETA | 6 pruebas nuevas de R-03 a R-09 y validaciones críticas; 359 pruebas totales |
| 8 — Cierre | COMPLETA | `README.md`, `SDD/Self-check-rubric.md`, matriz completa, verificaciones finales y este registro |

## Historial real de commits

```
093e9d2 docs(sdd): complete traceability and add self-check rubric
0ff7fd5 docs(readme): document architecture setup and endpoints
331f602 test(domain): enforce rich model encapsulation
88aef9d test(domain): cover damaged inventory sale and negative reconstitution
c2138fc test(domain): cover order immutability and invalid transitions
75d1509 docs(sdd): document mongodb 4.4 non-avx constraint
d293be0 docs(progress): record runtime fixes and local verification
73fb39d fix(config): use spring boot 4 mongodb property prefix
a90c6ef fix(docker): use mongodb 4.4 for non-avx hosts
fa895fa fix(persistence): avoid mysql reserved condition column
2429a88 fix(persistence): bind user role query with catalog type
df1a31a test(security): cover jwt, authorization, cors and bootstrap
9370dae fix(security): run request id filter before security context
c2b44e6 build(infra): add docker compose and profile configuration
8d6aeef fix(rest): resolve authenticated domain user from principal
1feb18e feat(config): wire domain services and bootstrap administrator
3fd9238 feat(security): add JWT filter, role rules, CORS and request id
016b386 feat(security): add JWT, password and clock adapters
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
.\mvnw.cmd clean test               # 359 tests OK, 0 fallos
```

## Verificaciones finales de Fase 8

| Verificación | Comando | Resultado |
|---|---|---|
| Pureza del dominio | `grep -RInE 'org\.springframework\|jakarta\.persistence\|com\.fasterxml' src/main/java/application/domain` | Sin coincidencias (exit 1) |
| Strings acotados | `grep -RInE 'private\s+String\s+(status\|type\|role\|condition\|movement)' src/main/java/application/domain/models` | Sin coincidencias (exit 1) |
| Dinero flotante | `grep -RInE 'private\s+(Double\|double\|Float\|float)\s+' src/main/java/application/domain src/main/java/application/adapters` | Sin coincidencias (exit 1) |
| Identificadores en español | `grep -RInE 'comprador\|vendedor\|bodega\|pedido\|factura\|envio\|devolucion\|reembolso' src` | Sin coincidencias (exit 1) |
| Trazabilidad | Elementos de `SDD/Traceability-Matrix.md` contra clases en `src/main/java` | Sin clases faltantes; solo quedan valores de catálogo, tipos Java y los puertos descartados por D-10 |
| Secretos | `.env`, `target/`, logs | `.env` ignorado; solo `.env.example` versionado |
| Archivo descalificante | `evaluacion1.md` | No existe |

El commit de cierre de esta fase se identifica con `git log -1`.

## Misión RVI — rama `feat/spec-coverage` (2026-11-05)

- Base: `main` `b5e899a`; `origin/develop` `0ea554f`. No hay merge ni push de la rama.
- PDF funcional extraído con PDFBox 2.0.31: 8 páginas, hash de texto
  `1C994B68FE6A237F7C67AA1AB98D5A47373E4E24D1690EDE082DB9C1C13109F5`.
- Ledger atómico: 84 requisitos `REQ-*` en `SDD/Spec-Ledger/ledger.md`;
  supuestos `SUP-01`…`SUP-15` en `SDD/Supuestos-no-verificados.md`.
- D-09 corregida y confirmada contra p.8 §12: las 4 columnas existen.
- `SDD/Gap-Analysis.md` concluye que el PDF no respalda 28 servicios/90 endpoints;
  F4 y F5 quedan N/A por falta de requisitos, sin inventar servicios.
- F3 corrige 9 defectos: transacciones en los 6 casos de uso, compensación de
  checkout, transiciones completas de despacho/entrega y autorización total de
  inventario. Tests nuevos: `UseCaseTransactionTest` y casos de fallo en
  `OrderServiceTest`/`InventoryServiceTest`.
- Verificación actual: **393 pruebas, 0 fallos, 0 errores, 0 omitidas** (59 suites).
## Misión D8 — N+1 en rutas de listado (2026-11-07)

- D8 (N+1): **RESUELTO**. `JpaDependencyResolver` resuelve en lote compradores,
  vendedores, productos, variantes, ítems, asignaciones, facturas, bodegas y
  devoluciones; `findAll`/`findByStatus` de pedidos, inventarios y devoluciones
  usan esas rutas. Commit `84d5b3d`.
- Regresión: `H2QueryCountRegressionTest` compara sentencias SQL con 2 y 6
  pedidos y exige el mismo número; falló antes del arreglo (15 → 31) y pasa
  después. Commit `9c6f376`.
- Corrección de compilación: se eliminó el BOM introducido al editar dos
  adaptadores; commit `7aa6618`.
- Índices agregados: `product_variants.product_id`,
  `order_allocations.product_id`, `returns.status` y
  `buyer_additional_addresses.buyer_id`.
- Paginación: **PENDIENTE Y DIFERIDA**. No se cambiaron puertos de dominio ni
  contratos REST.
- Suite completa: `.\mvnw.cmd clean test` → **413 pruebas, 0 fallos, 0 errores,
  0 omitidas; BUILD SUCCESS** en 03:50 min (2026-11-07).
- Últimos commits: `15159ae` (flujo de compra H2), `84d5b3d` (N+1),
  `9c6f376` (regresión), `7aa6618` (BOM).

## Misión D9 — cierre de verificación F6 (2026-11-08)

- DEF-04/DEF-05: `H2UseCaseRollbackTest` demuestra rollback de envío y de
  recepción de devolución ante fallos; commit `86c5258`.
- Arquitectura: `HexagonalArchitectureTest` exige que los controladores dependan
  de puertos de entrada y que el dominio no importe Spring/JPA/Jackson; commit
  `015c91d`.
- Auditoría: A-18 documenta que MongoDB queda fuera de la transacción JPA y que
  un fallo de auditoría revierte el trabajo de negocio; commit `0f72944`.
- F6 verificado: `@Version` en pedidos e inventarios; índices de consulta en
  pedidos, inventarios, asignaciones, productos, envíos, devoluciones y
  reembolsos; `SETUP.md` y `SECURITY.md` presentes; `.env`, `logs/` y `*.log`
  ignorados. `JpaMappingConstraintsTest` ahora cubre también `orders.status`,
  `inventories.warehouse_id`, `order_allocations.{order_id,product_id,warehouse_id}`,
  `products.status` y `returns.status`.
- Paginación: **PENDIENTE Y DIFERIDA**; no se cambiaron puertos ni contratos REST.
- Suite completa: `.\mvnw.cmd clean test` → **420 pruebas, 0 fallos, 0 errores,
  0 omitidas; BUILD SUCCESS** en 04:22 min (2026-11-08, 66 suites).
- Contextos Spring: 5 en total (4 `@DataJpaTest` + 1 `@WebMvcTest`), sin
  `@SpringBootTest`; `H2OrderPersistenceTest` reutiliza el contexto de
  `H2OptimisticLockingTest`. La suma de tiempos de suites es 136.0 s; el resto
  del tiempo corresponde a arranque de JVM/contextos y compilación.

## Misión docente — rama `fix/demo-docente` (2026-11-10)

- P1: `H2CartPersistenceTest` reproduce la violación `CART_ITEMS(CART_ID,
  PRODUCT_ID)` y confirma el `flush()` de `CartJpaAdapter.save`; commit `2fa19ee`.
- P2: `H2TeacherScenarioTest` compra de tres vendedores; el orden correcto es
  crear todos los envíos antes de iniciarlos; commit `b8f514b`.
- P3: devolución parcial por producto/cantidad, aditiva y retrocompatible;
  commit `a5b99bb`.
- P4: `CART_ITEM_ADDED/UPDATED/REMOVED/CLEARED` auditados sobre `CART`;
  commit `54edd4f`.
- P5: `scripts/demo-flujo.ps1` ejecuta el flujo completo contra la API y consulta
  la auditoría en MongoDB; commit `19c5a9a`.
- P6: requisito docente registrado como `[REQUISITO-DOCENTE]` en el ledger,
  supuestos y decisiones; documentación de `CartService` y `ReturnService` en
  este commit.
- Corrección post-demo: `ProductJpaAdapter.findById` usa
  `JpaDependencyResolver.productWithWarehouses`, así `Product.publish()` ve las
  bodegas del vendedor; los listados conservan el resolvedor shallow. El nuevo
  `H2PhysicalProductPublishTest` falla sin el arreglo con
  `SellerWithoutWarehouseException`, y `H2TeacherScenarioTest` ya publica por
  `CatalogService.publishProduct`.
- Suite completa: `.\mvnw.cmd test` → **429 pruebas, 0 fallos, 0 errores,
  0 omitidas; BUILD SUCCESS** en 69 suites (2026-11-10).
