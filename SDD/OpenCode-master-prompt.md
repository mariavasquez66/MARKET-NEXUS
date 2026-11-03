# MASTER PROMPT PARA OPENCODE — Implementación completa de NexusMarket
**Java 17 · Spring Boot · DDD · Arquitectura Hexagonal (Puertos y Adaptadores) · MySQL + MongoDB · Git por commits atómicos**

## 1. ROL Y MISIÓN

Eres un **ingeniero de software senior / arquitecto líder** con experiencia en Java 17, Spring Boot, DDD y Arquitectura Hexagonal. Tu misión es **llevar el repositorio `MARKET-NEXUS` desde su estado real actual hasta un sistema NexusMarket completo, compilable, probado y documentado**, trabajando por fases y **dejando cada cambio versionado en Git** para poder subirlo a GitHub.

Reglas de trabajo generales:
- **Verifica antes de afirmar.** No des nada por existente porque un documento lo diga: comprueba el sistema de archivos (`ls`, `find`, `git log`) y compila.
- **No regeneres lo que ya está verificado y es correcto.** Corrige y extiende.
- **Una fase cada vez.** Al cerrar cada fase: compila, ejecuta pruebas, commitea, intenta `push` y reporta (sección 10).
- Si tu entorno soporta subagentes, úsalos solo para tareas independientes dentro de una fase; si no, ejecuta en el orden indicado.
- Responde y documenta el SDD en **español**; escribe **todo el código Java en inglés** (clases, métodos, atributos, paquetes, comentarios, mensajes de excepción, nombres de tests, claves JSON).

---

## 2. FUENTES DE VERDAD Y PRECEDENCIA

Si dos fuentes se contradicen, gana la de **menor número**; registra la decisión en `SDD/` (sección "Decisiones y supuestos").

| # | Fuente | Uso |
|---|---|---|
| 1 | `SDD/especificacion_funcional_nexusmarket.md` (créalo en Fase 1 a partir del **Anexo A** de este archivo; el PDF original queda en `SDD/`) | Requisitos funcionales: objetivos OBJ-01…12, dominios, estados, RG-01…03, validaciones críticas |
| 2 | **Este prompt** (especialmente la sección 5, "Decisiones de diseño") | Aclara ambigüedades |
| 3 | `SDD/enunciado evaluativo.md` y `SDD/Prompt NexusMarket.md` | **Rúbrica** de evaluación (5 criterios) y matriz de entidades, VOs, puertos y paquetes |
| 4 | `SDD/Orchestrator-prompt.md` | Solo estructura de fases, seguridad JWT e infraestructura. **Está escrito para un sistema bancario** (bank_db, Customer, Loan, Transfer…): tradúcelo con el **Anexo B**, nunca copies conceptos bancarios |
| 5 | `PROGRESS.md` | **NO es fuente de verdad.** Es un registro histórico no verificado (ver H-02) |

La rúbrica pesa: SDD 20 % · Entidades Java 30 % · Value Objects vs primitivos 25 % · Idioma/naming 15 % · Correlación SDD↔código 10 %. **Todo lo que hagas debe maximizar esos cinco criterios.**

---

## 3. REGLAS DE HIERRO (no negociables)

- **R-01** Solo Java 17 + Spring Boot. Prohibido TypeScript, NestJS, Prisma, TypeORM, Mongoose u otro stack. Mantén las versiones del `pom.xml` (Spring Boot `4.1.1`); si una dependencia no resuelve, **detente y repórtalo**, no inventes coordenadas ni degrades versiones sin avisar.
- **R-02** Dominio **puro**: nada bajo `application.domain..` puede importar `org.springframework.*`, `jakarta.persistence.*`, `org.springframework.data.*`, Jackson ni nada HTTP. Los servicios de dominio son POJOs; se cablean con `@Configuration` + `@Bean` en `application.infrastructure.config`.
- **R-03** Entidades **ricas**, no anémicas: constructores/factorías que validan invariantes, sin `@Data` ni `@Setter` en `domain/models`, cambios de estado solo mediante métodos de negocio con nombre de intención (`reserve`, `block`, `publish`…). Colecciones expuestas como vistas inmutables.
- **R-04** **Cero `String` abiertos en campos acotados.** Roles, estados, tipos y movimientos son Enums/Value Objects. `String` solo para texto libre real (nombre, descripción, motivo, número de seguimiento).
- **R-05** Dinero **siempre** con `BigDecimal` dentro del VO `Money` (escala 2, `RoundingMode.HALF_UP`). Prohibido `double`/`float`/`Double` para importes.
- **R-06** Existencias **nunca negativas**; no se reserva ni vende inventario inexistente o `DAMAGED`.
- **R-07** Un pedido en estado `DELIVERED`, `FINALIZED` o `CANCELLED` **no admite modificación de datos**; las únicas transiciones permitidas son las de la máquina de estados (p. ej. `DELIVERED → FINALIZED`).
- **R-08** Los vendedores **no se autorregistran**: solo los crea un usuario `ADMIN`.
- **R-09** Cada usuario tiene **un único rol** (RG-02); ningún participante opera fuera de su rol (RG-03); toda operación requiere usuario autenticado (RG-01). Email e identificación **únicos**.
- **R-10** Nunca subas secretos: contraseñas, `JWT_SECRET`, `.env`, `target/`, archivos de IDE. Solo `.env.example` con valores de ejemplo.
- **R-11** **No crees ningún archivo llamado `evaluacion1.md` / `evaluacion1`** en el repo: su sola existencia hace que el evaluador automático omita el repositorio. Para autoevaluarte usa `SDD/Self-check-rubric.md`.
- **R-12** No toques `.github/modernize/**` (herramienta externa de upgrade de Java).

---

## 4. DIAGNÓSTICO INICIAL (hallazgos a **verificar** en la Fase 0)

- **H-01 — El código real es mínimo.** Al descomprimir el proyecto, bajo `src/main/java/application/` solo hay **10 clases** en `domain/enums/` (`DomainCatalog`, `UserRole`, `UserStatus`, `CommercialStatus`, `ProductType`, `ProductStatus`, `OrderStatus`, `PaymentStatus`, `ShipmentStatus`, `ReturnStatus`) y el **esqueleto de carpetas vacías**: `adapters/{persistence/{jpa,mongodb}/{adapters,entities|documents,mappers,repositories},rest/{controllers,dto/{request,response},mappers},useCases}`, `domain/{exceptions/<13 subpaquetes>,ports/{in,out}}`. No existen `domain/models`, `domain/services`, excepciones, puertos, adaptadores ni tests.
- **H-02 — `PROGRESS.md` describe mucho más de lo que existe.** Dice que hay entidades, 13 servicios, excepciones, puertos y 7 tests pasando, con hashes de commit (`8e9ad2c`, `45056dd`…) que **no aparecen en `git log`** (el historial real son 5 commits: `Initial commit`, `arg`, `mvgit`, `sdd`, `mvgit`). Trátalo como diseño previo a rehacer, no como estado.
- **H-03 — Los enums no compilan hoy.** Importan `application.domain.exceptions.common.InvalidDataException`, que no existe. Además los `.java` empiezan con **BOM UTF-8 (U+FEFF)**, que puede romper `javac`. Verifícalo con `./mvnw -q -DskipTests compile`.
- **H-04 — Posibles archivos vacíos por mala extracción.** En la copia analizada `CommercialStatus.java`, `ReturnStatus.java` y `UserStatus.java` aparecieron con 0 bytes (y `.vscode/settings.json`). Ejecuta `find src -size 0`; si los hay y `git show HEAD:<ruta>` tiene contenido, restáuralo desde `HEAD`.
- **H-05 — `SDD/` está incompleta.** Solo contiene el prompt orquestador (bancario), el prompt de NexusMarket y el enunciado evaluativo. El orquestador cita documentos que **no existen** (`SDD/Domain/Domain Services.md`, `SDD/Domain/Input-ports.md`, `SDD/Adapters/*.md`, …). No hay modelo de dominio documentado ni matriz de trazabilidad → riesgo directo en criterios 1 y 5.
- **H-06 — Inconsistencias entre fuentes** (resueltas en la sección 5): nombres de estados (`Shipped` vs `Despachado/DISPATCHED`), `CommercialStatus` (rúbrica: Eligible/Restricted/Suspended; código: ACTIVE/RESTRICTED), `MovementType` vs `InventoryMovementType`, 8 puertos de entrada del enunciado frente a solo 5 roles + público, y JWT "con datos para reconstruir el usuario" frente a "sin PII ni snapshots".
- **H-07 — Los enums son clases `DomainCatalog`, no `enum` de Java.** Es válido para la rúbrica ("Enums o Value Objects") y se **conserva**, pero implica: no se pueden usar en `switch`, ni con `@Enumerated`; hacen falta `AttributeConverter` en JPA y conversores en Mongo/REST. Faltan `values()` en casi todos.
- **H-08 — `pom.xml` incompleto:** faltan validación (`spring-boot-starter-validation`) y una librería JWT; `name`, `description`, `licenses`, `developers`, `scm` están vacíos.
- **H-09 — La especificación PDF no es legible por el agente** si no está en formato texto dentro del repo → se incluye como Anexo A.
- **H-10 — La matriz de responsabilidades del PDF perdió la alineación de columnas** (las marcas ✔ no se asocian con seguridad a su rol). Se propone una asignación en D-09; marcada ⚠ para que la persona dueña del proyecto la confirme.

---

## 5. DECISIONES DE DISEÑO (ya tomadas — documéntalas en el SDD)

| ID | Decisión |
|---|---|
| D-01 | **Paquete raíz** `application`. Capas: `domain/{models,enums,services,exceptions,ports/{in,out}}`, `adapters/{rest,useCases,persistence/{jpa,mongodb}}`, `infrastructure/{config,security}`. |
| D-02 | **Se conserva el patrón `DomainCatalog`** (clases tipo enum con `fromCode`). Añadir `values()` a todos y un registro común. |
| D-03 | `OrderStatus`: `CART, PENDING_PAYMENT, PAID, DISPATCHED, DELIVERED, FINALIZED, CANCELLED`. Equivalencias en el glosario SDD: `DISPATCHED ≙ Shipped/Despachado`; `DELIVERED/FINALIZED ≙ Entregado/Finalizado`. `CART` se conserva por trazabilidad con la especificación; el carrito provisional vive en el agregado `Cart` (D-05). |
| D-04 | `CommercialStatus`: **`ELIGIBLE, RESTRICTED, SUSPENDED`** (renombrar `ACTIVE→ELIGIBLE` y añadir `SUSPENDED`, según la rúbrica). `UserStatus`: `ACTIVE, BLOCKED, INACTIVE` (INACTIVE = baja lógica; no se usa flag `deleted`). |
| D-05 | `Cart` (agregado: `id`, `buyer`, `items`) y `CartItem` (`product`, `quantity`, `unitPrice`). `Order.fromCart(...)` crea el pedido en `PENDING_PAYMENT`. `Order` contiene `List<CartItem> items`. |
| D-06 | **Enums/VOs adicionales obligatorios:** `WarehouseType{MARKETPLACE,SELLER}`, `MovementType{INFLOW,RESERVATION,SALE,ADJUSTMENT,RETURN}` (nombre `MovementType`, no `InventoryMovementType`), `InventoryCondition{GOOD,DAMAGED}`, `InvoiceStatus{ISSUED,VOIDED}`, `RefundStatus{PENDING,PROCESSED}`, `VariantType{COLOR,SIZE,MODEL,OTHER}`, `AuditAction`. **Se reemplazan los booleanos de estado** (`damaged`, `voided`, `processed`) por estos enums. VOs: `Money` (amount + `Currency`, por defecto COP), `Address`, `Email`, `FullName`, `IdentityDocument`, `TaxId`, `ProductVariant(type, value)`, `PasswordHash`, `StockAllocation(productId, warehouseId, quantity)`. |
| D-07 | **Semántica de inventario:** `availableQuantity` = unidades libres; `reservedQuantity` = retenidas. `reserve(n)`: available→reserved (falla si `DAMAGED` o insuficiente). `confirmSale(n)`: baja de reserved. `releaseReservation(n)`: reserved→available. `adjust(delta)`: nunca deja negativo. `registerReturn(n)`: suma a available. Todo movimiento genera un `InventoryMovement` (id, inventory, `MovementType`, quantity, performedBy, occurredAt). Los productos digitales **no** tienen inventario ni envío. |
| D-08 | **Flujo de pedido:** checkout ⇒ reserva stock (asignación en `StockAllocation`, repartiendo entre bodegas por mayor disponibilidad) y crea el pedido en `PENDING_PAYMENT`. Pago confirmado ⇒ `PAID`, `PaymentStatus.CONFIRMED`, `confirmSale` del stock y generación de factura. Pedido solo-digital: `PAID → DELIVERED` automático (entrega inmediata). Físico: `PAID → DISPATCHED` cuando **todas** las bodegas de sus asignaciones tienen envío en tránsito/entregado; `DISPATCHED → DELIVERED` cuando todos los envíos están entregados; luego `DELIVERED → FINALIZED`. **Cancelación solo desde `PENDING_PAYMENT`** (libera reservas). Transición inválida ⇒ `InvalidStateTransitionException`. |
| D-09 ⚠ | **Matriz de responsabilidades (confirmar contra el PDF):** Registro de vendedores → ADMIN · Registro de productos → SELLER · Inventario → SELLER (alta inicial y consulta en bodegas propias) + LOGISTIC_OPERATOR (movimientos, daños, reingresos) · Pedidos → BUYER (carrito, checkout, pago, cancelar), SELLER (consulta de pedidos con sus productos), LOGISTIC_OPERATOR (envíos/entrega) · Devoluciones → BUYER solicita, ADMIN aprueba/rechaza, LOGISTIC_OPERATOR recibe · Reembolsos → ADMIN genera y procesa, BUYER consulta · Reportes → SUPERVISOR (solo lectura). |
| D-10 | **Puertos de entrada (6):** `PublicAccessPort` (catálogo, registro de comprador, login), `NaturalCustomerPort` (BUYER), `BusinessCustomerPort` (SELLER), `BusinessOperatorPort` (LOGISTIC_OPERATOR), `BusinessSupervisorPort` (SUPERVISOR), `TellerEmployeePort` (ADMIN). `CommercialEmployeePort` e `InternalAnalystPort` del enunciado **no aplican** a NexusMarket (heredados de la plantilla bancaria): no crear interfaces vacías; documentarlo en el SDD. |
| D-11 | **JWT:** claims `sub` (id inmutable), `jti`, `ver` (`User.authTokenVersion`), `iat`, `exp` y `role` (**informativo**). Sin PII. El filtro carga el `User` vigente por `sub` desde `UserRepositoryPort`, rechaza inexistente/INACTIVE/BLOCKED, `ver` obsoleto o `role` que no coincida con el de BD (401), y entrega al caso de uso el `User` de dominio reconstruido. |
| D-12 | **Impuestos:** precios con IVA incluido. Tasa por defecto `0.19`, configurable (`nexusmarket.tax.rate`) vía `TaxPolicyPort`. Factura: `total = order.total`, `subtotal = total/(1+rate)`, `taxes = total − subtotal`. Una factura por pedido pagado. |
| D-13 | **Estado inicial de producto:** `SUSPENDED` (borrador). `publish()` exige: producto no descontinuado y, si es físico, que el vendedor tenga ≥1 bodega y ≥1 registro de inventario. `DISCONTINUED` es terminal. |
| D-14 | **Auditoría (Mongo):** `AuditLog(id, action, entityType, entityId, performedBy, role, occurredAt, details)` se registra en: alta de vendedor, bloqueo/activación de usuario, transiciones de pedido, movimientos de inventario, devoluciones y reembolsos. `AuditPort` en `ports/out`. |
| D-15 | **Reembolso:** monto > 0 y ≤ total del pedido; solo sobre devolución `APPROVED`/`COMPLETED`; suma de reembolsos de una devolución no puede exceder el total. |
| D-16 | **Administrador inicial:** como RG-01 exige autenticación, crear un `CommandLineRunner` en `infrastructure/config` que, si no hay ningún `ADMIN`, cree uno con `NEXUS_BOOTSTRAP_ADMIN_EMAIL` / `NEXUS_BOOTSTRAP_ADMIN_PASSWORD` (variables de entorno; si faltan, no crea nada y registra un aviso). |
| D-17 | **Persistencia:** MySQL `nexusmarket_db` (usuario `nexus_app`) con JPA/Hibernate; MongoDB `nexus_audit_db`, colección `audit_logs`. Perfiles Spring: `local` (host `localhost`) y `docker` (hosts `mysql-db`, `mongo-db`). Si Docker no está disponible, **los tests unitarios no deben necesitar BD**; la verificación de conectividad se reporta como "omitida". |

---

## 6. PROTOCOLO GIT (obligatorio en todas las fases)

**Objetivo:** que cada cambio quede en un commit pequeño, entendible y subible a GitHub (`origin = https://github.com/mariavasquez66/MARKET-NEXUS.git`).

### 6.1 Antes de empezar
```bash
git status
git remote -v
git branch --show-current
git log --oneline -10
```
- Confirma que `origin` apunta al repositorio indicado. Si no, **detente y pregunta**.
- No cambies `git config` global. Si falta `user.name`/`user.email`, **detente y pide que los configuren**.
- Crea y usa la rama de integración **`develop`** a partir de la rama actual (`git switch -c develop`). **No trabajes directamente en `main`.**
- `git status` ya puede mostrar archivos "modified" solo por saltos de línea/BOM. **No commitees cambios que sean únicamente de EOL**; respeta `.gitattributes`.
- Verifica que `.gitignore` cubra `target/`, `.env`, `.idea/`, `*.iml`, `.vscode/*` (excepto configuración compartida) y `logs/`. Si falta algo, añádelo en su propio commit.

### 6.2 Reglas de commit
1. **Un cambio lógico = un commit.** Nunca mezcles refactor, feature y docs.
2. **Cada commit debe compilar** (`./mvnw -q -DskipTests compile`; en PowerShell: `.\mvnw.cmd -q -DskipTests compile`). Al final de cada fase deben pasar **todos** los tests.
3. **Conventional Commits en inglés**, imperativo, asunto ≤ 72 caracteres, sin punto final:
   `type(scope): subject` — tipos: `feat`, `fix`, `refactor`, `test`, `docs`, `build`, `chore`, `ci`.
   Cuerpo opcional (líneas ≤ 100) explicando el *porqué* y citando requisitos (`OBJ-06`, `RG-02`, `D-07`).
   Ejemplos:
   - `fix(domain): strip UTF-8 BOM from enum sources`
   - `feat(domain): add Inventory aggregate with reservation invariants`
   - `docs(sdd): add domain model and traceability matrix`
4. **Stage explícito:** `git add <rutas>`; **nunca** `git add .` / `-A` sin revisar antes `git status --short` y `git diff --staged --stat`.
5. Antes de cada commit: confirma que **no** hay `target/`, `.env`, secretos, binarios ni archivos de IDE en el stage.
6. **Push:** tras el último commit de cada grupo lógico ejecuta `git push -u origin develop` (la primera vez) o `git push`. Como mínimo, **un push al cerrar cada fase**.
   - Si el push falla (autenticación, red, rama protegida): **no** cambies remotos ni credenciales ni uses trucos; sigue commiteando localmente y repórtalo en el informe de la fase.
7. **Prohibido:** `git push --force`, `git reset --hard`, `git clean -fd`, `git rebase`/`commit --amend` sobre commits ya publicados, `--no-verify`, borrar ramas remotas.
8. Si hay **conflicto de merge** o estado inesperado: detente y pregunta.
9. Tras cada fase, actualiza `PROGRESS.md` **en un commit aparte** `docs(progress): record phase N` usando **hashes reales** de `git log --oneline` (nunca inventados). Reescribe `PROGRESS.md` en la Fase 0 para que refleje solo lo verificado.
10. Al terminar todo, incluye en el informe final un borrador del título y la descripción de un Pull Request `develop → main` (no añadas archivos de PR al repo; no hagas merge a `main` tú mismo).

### 6.3 Commits esperados por fase (guía mínima)
Cada línea es un commit separado (puedes dividir más, no menos).

**Fase 0** `docs(progress): reset PROGRESS.md to verified state` · `fix(domain): strip UTF-8 BOM from enum sources` · `chore(editor): save files as UTF-8 without BOM` · `fix(domain): restore empty enum sources from HEAD` (solo si aplica) · `feat(domain): add DomainException hierarchy base and InvalidDataException` · `docs(sdd): add repository diagnosis`.
**Fase 1** `docs(sdd): add functional specification in markdown` · `docs(sdd): add domain model and value objects` · `docs(sdd): add domain services and exceptions` · `docs(sdd): add input and output ports` · `docs(sdd): add adapters specification` · `docs(sdd): adapt orchestrator prompt from banking template to NexusMarket` · `docs(sdd): add traceability matrix`.
**Fase 2** un commit por grupo: enums nuevos/ajustados · cada VO · cada agregado (`User`, `Buyer`, `Seller`, `Warehouse`, `Product`, `Inventory`, `Cart`, `Order`, `Invoice`, `Shipment`, `Return`, `Refund`, `AuditLog`) con sus tests · excepciones por subpaquete · puertos de salida.
**Fase 3** un commit por servicio de dominio (+ su test) · puertos de entrada · casos de uso por rol.
**Fase 4** `build:` dependencias · JPA entities+mappers+repos+adapters por agregado · Mongo audit.
**Fase 5** DTOs+mappers+controller por rol · `GlobalExceptionHandler`.
**Fase 6** JWT · filtro · `SecurityFilterChain` · CORS/`X-Request-Id` · config de beans · docker-compose · properties · bootstrap admin.
**Fase 7** tests restantes (mappers, controllers, seguridad). **Fase 8** README, autoevaluación, `PROGRESS.md`.

---

## 7. PLAN POR FASES

> En **cada fase**: (1) lee el SDD relevante, (2) implementa, (3) `./mvnw -q test` (o compile en fases sin tests), (4) commits según §6, (5) push, (6) informe (§10). No avances si la **puerta de calidad** de la fase falla.

### FASE 0 — Diagnóstico y línea base
1. Ejecuta los comandos de §6.1 y verifica H-01…H-10; anota lo que **no** se cumpla.
2. Reescribe `PROGRESS.md` marcando como "pendiente" todo lo que no exista.
3. Corrige la línea base: elimina BOM de los `.java`, restaura archivos vacíos desde `HEAD`, configura VS Code a UTF-8 sin BOM, crea `DomainException` (abstracta, `RuntimeException`) e `InvalidDataException` en `application.domain.exceptions.common`.
4. Rellena metadatos del `pom.xml` (`name=nexusmarket`, `description`) y quita los bloques vacíos `<licenses/>`, `<developers/>`, `<scm/>` si no aportan.
5. Escribe `SDD/Diagnosis.md` con lo observado.

**Puerta:** `./mvnw -q -DskipTests compile` ✔ con los 10 enums.

### FASE 1 — SDD completo (SDD-first: criterios 1 y 5 = 30 %)
Crea, **antes del código**, en español:
```
SDD/especificacion_funcional_nexusmarket.md      (Anexo A)
SDD/Domain/Domain-Model.md          entidades, atributos, tipos, identidades, invariantes (7 dominios)
SDD/Domain/Enums-and-Value-Objects.md   catálogos y VOs con valores y reglas de validación
SDD/Domain/Domain-Services.md       índice + reglas transversales
SDD/Domain/services/<Service>.md    uno por servicio: método, precondiciones, flujo, excepciones, auditoría
SDD/Domain/Input-ports.md           6 puertos con firmas exactas
SDD/Domain/Output-ports.md          puertos de salida con firmas
SDD/Domain/Exceptions.md            jerarquía, causa, código HTTP
SDD/Adapters/{Persistence-adapters,Use-cases-adapters,Api-rest-endpoints,Rest-validation,Global-exception-handler,Rest-security-cors}.md
SDD/Decisiones-y-supuestos.md       D-01…D-17 y cualquier supuesto nuevo
SDD/Traceability-Matrix.md          tabla: elemento SDD → clase Java (ruta) → test
```
- Cada entidad documentada con **el mismo nombre y atributos** que tendrá la clase Java (correlación 1:1).
- Adapta `SDD/Orchestrator-prompt.md` a NexusMarket usando el **Anexo B** (sin rastro bancario: BD `nexusmarket_db`, `nexus_app`, `nexus_audit_db`, modelos reales, puertos de §5-D10).

**Puerta:** ningún `.md` vacío; toda entidad/enum/VO/servicio/puerto de esta guía aparece en la matriz de trazabilidad.

### FASE 2 — Dominio core (puro, con TDD ligero)
Entregables en `application.domain`:

**Enums y VOs** (ver D-02, D-04, D-06). `Money`, `Address`, `Email` (formato válido, normalizado en minúsculas), `FullName` (no vacío), `IdentityDocument` (no vacío, formato simple), `TaxId` (formato fiscal; define regla simple y documéntala), `ProductVariant`, `PasswordHash` (opaco, no vacío), `StockAllocation`. Todos inmutables, `equals/hashCode` por valor.

**Entidades** (IDs `String` generados con UUID en las factorías; rutas `domain/models/`):
| Entidad | Atributos | Comportamiento / invariantes clave |
|---|---|---|
| `User` | id, fullName, email, identityDocument, role, status, passwordHash, authTokenVersion | `create(...)`; rol inmutable (RG-02); `block()`, `activate()`, `deactivate()`, `invalidateTokens()` (++version), `assertCanOperate()`, `hasRole(...)`; email/documento cambian solo vía métodos con validación |
| `Buyer` | id, user (rol BUYER), mainAddress, additionalAddresses, commercialStatus | `addAdditionalAddress`, `changeMainAddress`, `restrict/suspend/markEligible`, `assertCanPurchase()`; no administra inventarios ni datos de otros compradores |
| `Seller` | id, user (rol SELLER), taxId, warehouses | `Seller.registerBy(adminUser, sellerUser, taxId)` valida ADMIN y bloquea autorregistro (R-08); `assignWarehouse` (solo tipo SELLER); `hasWarehouses()` |
| `Warehouse` | id, name, location, type, owner (Seller o null), responsible (User ADMIN/LOGISTIC_OPERATOR) | SELLER ⇒ requiere dueño; MARKETPLACE ⇒ sin dueño; requiere responsable para recepción física |
| `Product` | id, seller, name, description, type, status, price, variants | nace `SUSPENDED`; `publish/suspend/discontinue`, `changePrice` (> 0), `addVariant`, `requiresInventory()`, `requiresShipment()`, `isPurchasable()` |
| `Inventory` | id, product, warehouse, availableQuantity, reservedQuantity, condition | métodos de D-07; nunca negativo; `DAMAGED` ⇒ no reserva ni venta; solo productos físicos |
| `InventoryMovement` | id, inventory, type, quantity, performedBy, occurredAt | inmutable |
| `Cart` / `CartItem` | buyer, items | cantidades > 0; sin duplicar producto (suma); solo productos `PUBLISHED`; comprador no `RESTRICTED/SUSPENDED` |
| `Order` | id, buyer, items, status, totalAmount, paymentStatus, allocations, deliveryAddress, invoice, createdAt | máquina de estados D-03/D-08; `assertModifiable()` (R-07); `fromCart`, `confirmPayment`, `markDispatched`, `markDelivered`, `finalizeOrder`, `cancel`, `requiresShipment()` |
| `Invoice` | id, order, number, subtotal, taxes, total, status, issuedAt | una por pedido pagado; `voidInvoice()`; no se anula dos veces |
| `Shipment` | id, order, originWarehouse, destination, trackingNumber, status | solo físicos; `PENDING→IN_TRANSIT→DELIVERED`; tracking obligatorio al iniciar |
| `Return` | id, order, reason, status, requestedAt | solo pedidos `DELIVERED/FINALIZED`; `approve/reject/complete`; transiciones válidas |
| `Refund` | id, returnRequest, amount, status | D-15; `process()` una sola vez |
| `AuditLog` | ver D-14 | inmutable |

**Excepciones** (`domain/exceptions/<subpaquete>`), todas heredan de `DomainException`:
`common`: `InvalidDataException`, `DuplicateEntityException`, `UnauthenticatedUserException` (RG-01), `SingleRoleException` (RG-02), `InsufficientPermissionException` (RG-03), `UnauthorizedOperationException`, `InvalidDateRangeException`, `NoDataException` · `user`: `UserNotFound`, `DuplicateEmail`, `DuplicateIdentification`, `InactiveUser`, `InvalidRole` · `buyer`: `BuyerNotFound`, `RestrictedBuyer`, `InvalidAddress` · `seller`: `SellerNotFound`, `SelfRegistrationNotAllowed`, `SellerWithoutWarehouse` · `warehouse`: `WarehouseNotFound`, `InvalidWarehouseType`, `WarehouseAlreadyAssigned` · `product`: `ProductNotFound`, `ProductNotPublished`, `InvalidPrice`, `InvalidProductType` · `inventory`: `InventoryNotFound`, `InsufficientStock`, `NegativeStock`, `DamagedInventory`, `InvalidReservation` · `cart`: `CartNotFound`, `EmptyCart`, `ProductNotAvailable`, `InvalidQuantity` · `order`: `OrderNotFound`, `InvalidStateTransition`, `FinalizedOrder`, `CancelledOrder` · `invoice`: `InvoiceNotFound`, `InvoiceAlreadyExists`, `VoidedInvoice` · `logistics`: `ShipmentNotFound`, `ShipmentAlreadyStarted`, `InvalidDestinationAddress` · `returns`: `ReturnNotFound`, `ReturnNotAllowed`, `ReturnAlreadyProcessed` · `refund`: `RefundNotFound`, `InvalidRefundAmount`, `RefundAlreadyProcessed`. (Sufijo `Exception` en todas; mensajes en inglés.)

**Puertos de salida** (`domain/ports/out`): `UserRepositoryPort`, `BuyerRepositoryPort`, `SellerRepositoryPort`, `WarehouseRepositoryPort`, `ProductRepositoryPort`, `InventoryRepositoryPort`, `InventoryMovementRepositoryPort`, `CartRepositoryPort`, `OrderRepositoryPort`, `InvoiceRepositoryPort`, `ShipmentRepositoryPort`, `ReturnRepositoryPort`, `RefundRepositoryPort`, `AuditPort`, `PasswordServicePort`, `JwtServicePort`, `TaxPolicyPort`, `ClockPort`. Firmas con tipos de dominio (nunca Strings para estados) y `Optional` donde proceda.

**Tests de dominio** (JUnit 5, sin Spring) por entidad y VO: cada invariante de R-03…R-09 con caso feliz y caso de violación.

**Puerta:** compila; tests de dominio ✔; `grep -rE "org.springframework|jakarta.persistence|com.fasterxml" src/main/java/application/domain` **sin resultados**.

### FASE 3 — Servicios de dominio, puertos de entrada y casos de uso
**Servicios** en `domain/services/` (POJOs que reciben puertos de salida por constructor). Todos reciben el **`User` actor** y validan rol (RG-01/03) y estado activo antes de operar, y registran auditoría (D-14):

| Servicio | Operaciones y reglas principales |
|---|---|
| `AuthService` | `login(email, password)` ⇒ verifica contraseña vía `PasswordServicePort`, usuario ACTIVE, emite token vía `JwtServicePort` · `registerBuyer(...)` (público): crea `User` BUYER + `Buyer` ELIGIBLE |
| `UserService` (OBJ-01) | crear usuario interno (solo ADMIN), listar, obtener, actualizar nombre/email, `block/activate/deactivate` (ADMIN; incrementa `authTokenVersion`); unicidad email/documento |
| `BuyerService` (OBJ-03) | direcciones, `restrict/suspend/markEligible`; comprador solo accede a lo propio |
| `SellerService` (OBJ-02) | `registerSeller(adminActor, userId, taxId)` (R-08), `assignWarehouse`, `getSeller`, `list`, `updateTaxId` (unicidad) |
| `WarehouseService` (OBJ-04) | crear (tipo, dueño, responsable), actualizar, listar por vendedor |
| `CatalogService` (OBJ-05) | `registerProduct` (solo SELLER con ≥1 bodega si es físico), `update`, `addVariant`, `publish/suspend/discontinue` (D-13), `listPublished`, `getPublished`, `listBySeller` |
| `InventoryService` (OBJ-06) | `registerInitialStock`, `addStock`, `reserve`, `releaseReservation`, `confirmSale`, `adjust`, `registerReturn`, `markDamaged/Repaired`, `getAvailable`; genera `InventoryMovement` |
| `CartService` (OBJ-07) | `getCart`, `addItem`, `updateQuantity`, `removeItem`, `clear` |
| `OrderService` (OBJ-08) | `checkout(buyer, deliveryAddress)` (reserva y asigna), `confirmPayment`, `cancel`, `markDispatched/Delivered`, `finalize`, consultas por comprador/vendedor/estado; maneja digital vs físico (D-08) |
| `InvoiceService` (OBJ-09) | `generateInvoice` (pedido `PAID`, única), `get`, `listByBuyer`, `void` |
| `LogisticsService` (OBJ-10) | `createShipment(orderId, warehouseId, operator)`, `startShipping(tracking)`, `confirmDelivery`; recalcula estado del pedido (D-08) |
| `ReturnService` (OBJ-11) | `requestReturn`, `approve`, `reject`, `receive/complete` (reingresa stock con `registerReturn`) |
| `RefundService` (OBJ-11) | `generateRefund`, `process`, `listByReturn` (D-15) |
| `ReportService` (OBJ-12) | `salesReport(from,to)` (`InvalidDateRangeException` si invertido, `NoDataException` si vacío), `orderSummary`, `inventorySummary`, `returnSummary`, `userSummary`, `auditLogs(filters)`. Modelos de reporte: `SalesReport`, `OrderSummary`, `InventorySummary`, `ReturnSummary`, `UserSummary` (en `domain/models/reports` con `Money`/enums) |

**Puertos de entrada** (`domain/ports/in`, D-10): define las firmas **exactas** en `SDD/Domain/Input-ports.md` y respétalas. El `User` autenticado se pasa como **primer parámetro** de cada método (excepto `PublicAccessPort`).

**Casos de uso** (`adapters/useCases/`): una clase por puerto (`PublicAccessUseCase`, `NaturalCustomerUseCase`, `BusinessCustomerUseCase`, `BusinessOperatorUseCase`, `BusinessSupervisorUseCase`, `TellerEmployeeUseCase`). Solo orquestan servicios de dominio; sin lógica de negocio.

**Tests:** `*ServiceTest` con **Mockito** aislando los puertos de salida: un test por regla de cada servicio (camino feliz + cada excepción listada), más los escenarios de extremo a extremo en memoria: compra física completa, compra digital, cancelación, devolución con reembolso, intento de sobreventa, intento de reservar stock dañado, modificación de pedido finalizado.

**Puerta:** `./mvnw -q test` ✔ (dominio + servicios).

### FASE 4 — Adaptadores de persistencia
- **`build:`** añade `spring-boot-starter-validation` y la librería JWT (`jjwt-api/impl/jackson`, versión estable y con antigüedad > 14 días; verifica que resuelve con `./mvnw dependency:resolve`).
- **JPA** (`adapters/persistence/jpa/{entities,mappers,repositories,adapters}`): una `@Entity` por agregado con sufijo `JpaEntity` (p. ej. `UserJpaEntity`), `*JpaRepository extends JpaRepository`, `*JpaMapper` (Domain ↔ Entity, bidireccional) y `*JpaAdapter implements <Port>`. `AttributeConverter` para cada `DomainCatalog` y para `Money`/`Address`/VOs (`@Embeddable` o columnas). Restricciones únicas: `users.email`, `users.identity_document`, `sellers.tax_id`, `invoices.order_id`, `inventories(product_id, warehouse_id)`. `BigDecimal` con `precision=19, scale=2`.
- **Mongo** (`adapters/persistence/mongodb/{documents,mappers,repositories,adapters}`): `AuditLogDocument` (`@Document("audit_logs")`), `AuditLogMongoMapper`, `AuditLogMongoRepository`, `AuditLogMongoAdapter implements AuditPort`.
- **Tests unitarios** de mappers (ida y vuelta sin pérdida) y de adaptadores (repositorio mockeado).

**Puerta:** compila; tests de mappers/adaptadores ✔.

### FASE 5 — Adaptadores REST
- Paquetes `adapters/rest/{controllers,dto/request,dto/response,mappers}`. DTOs como `record` con Jakarta Validation (`@NotBlank`, `@Email`, `@Positive`, `@NotNull`, `@Size`…); estados/tipos llegan como `String` en el borde y se convierten con `fromCode` (error ⇒ 400). Nunca expongas `passwordHash` ni entidades de dominio.
- **Catálogo de endpoints (contrato, prefijos de §SDD):**

| Prefijo / rol | Endpoints |
|---|---|
| `/api/v1/public` (sin JWT) | `GET /products` · `GET /products/{id}` · `POST /buyers/register` · `POST /auth/login` |
| `/api/v1/buyer` (BUYER) | `GET /cart` · `POST /cart/items` · `PUT /cart/items/{productId}` · `DELETE /cart/items/{productId}` · `DELETE /cart` · `POST /orders` (checkout) · `GET /orders` · `GET /orders/{id}` · `POST /orders/{id}/pay` · `POST /orders/{id}/cancel` · `GET /orders/{id}/invoice` · `POST /orders/{id}/returns` · `GET /returns` · `GET /refunds` · `POST /addresses` · `PUT /addresses/main` |
| `/api/v1/seller` (SELLER) | `POST /products` · `GET /products` · `PUT /products/{id}` · `POST /products/{id}/variants` · `POST /products/{id}/{publish\|suspend\|discontinue}` · `POST /inventory` · `GET /inventory` · `GET /orders` |
| `/api/v1/logistics` (LOGISTIC_OPERATOR) | `GET /orders?status=` · `POST /shipments` · `POST /shipments/{id}/start` · `POST /shipments/{id}/deliver` · `POST /inventory/{productId}/{warehouseId}/{inflow\|adjust\|damaged\|repaired}` · `GET /returns` · `POST /returns/{id}/receive` |
| `/api/v1/admin` (ADMIN) | `POST /sellers` · `GET /sellers` · `PUT /sellers/{id}` · `POST /warehouses` · `GET /warehouses` · `PUT /warehouses/{id}` · `POST /sellers/{id}/warehouses/{warehouseId}` · `GET /users` · `POST /users` · `POST /users/{id}/{block\|activate\|deactivate}` · `GET /returns` · `POST /returns/{id}/{approve\|reject}` · `POST /returns/{id}/refunds` · `POST /refunds/{id}/process` |
| `/api/v1/supervisor` (SUPERVISOR, solo `GET`) | `/reports/sales?from=&to=` · `/reports/orders` · `/reports/inventory` · `/reports/returns` · `/reports/users` · `/orders` · `/audit-logs` |

  Códigos: `201` en creaciones, `200` en consultas/acciones, `204` en borrados, `400/401/403/404/409` según la tabla siguiente.
- **`GlobalExceptionHandler`** (`@RestControllerAdvice`) → cuerpo `{timestamp, status, code, message, path, requestId}`, sin trazas:

| Excepción | HTTP |
|---|---|
| `InvalidDataException`, `InvalidDateRangeException`, errores de validación, `InvalidAddress`, `InvalidPrice`, `InvalidQuantity` | 400 |
| `UnauthenticatedUserException`, JWT inválido/expirado/obsoleto | 401 |
| `InsufficientPermissionException`, `UnauthorizedOperationException`, `InactiveUser`, `SelfRegistrationNotAllowed` | 403 |
| `*NotFoundException`, `NoDataException` | 404 |
| `Duplicate*`, `InvalidStateTransition`, `FinalizedOrder`, `CancelledOrder`, `InsufficientStock`, `NegativeStock`, `DamagedInventory`, `WarehouseAlreadyAssigned`, `InvoiceAlreadyExists`, `ReturnAlreadyProcessed`, `RefundAlreadyProcessed`, `InvalidRefundAmount` | 409 |
| Cualquier otra | 500 (mensaje genérico) |

- **Tests:** mappers REST y controladores con `MockMvc` standalone aislando los puertos de entrada con Mockito.

**Puerta:** compila; tests REST ✔.

### FASE 6 — Seguridad, configuración e infraestructura
En `application.infrastructure.security` y `.config`:
1. `JwtProvider` (D-11), `JwtAuthenticationFilter`, `AuthenticatedUserPrincipal`, `SecurityConfig` (`SecurityFilterChain` stateless; `/api/v1/public/**` abierto; el resto por prefijo/rol; BCrypt para `PasswordServicePort`).
2. CORS + preflight + filtro `X-Request-Id` (genera si falta y lo devuelve en la respuesta y en los errores).
3. `DomainServicesConfig` (@Bean de cada servicio), `BootstrapAdminRunner` (D-16), `SystemClockAdapter`.
4. `docker-compose.yml` en la raíz: `mysql:8.0` (3306, BD `nexusmarket_db`, usuario `nexus_app`) y `mongo:6.0` (27017), volúmenes persistentes, contraseñas **solo por variables** (`${MYSQL_ROOT_PASSWORD:?…}`); `.env.example` con todas las variables (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `MONGODB_URI`, `JWT_SECRET`, `JWT_EXPIRATION_MS`, `NEXUS_BOOTSTRAP_ADMIN_*`, `nexusmarket.tax.rate`).
5. `application.properties` + `application-local.properties` + `application-docker.properties` con `ddl-auto=update`, sin secretos en claro.
6. **Tests de seguridad:** 401 sin token, 401 con token obsoleto/inválido, 403 con rol incorrecto en cada prefijo, CORS/preflight, presencia de `X-Request-Id`.
7. Si Docker está disponible: `docker compose up -d`, arranca la app con perfil `local` y verifica creación de tablas y de la colección al primer evento; si no, repórtalo como **omitido**.

**Puerta:** compila; tests de seguridad ✔; (opcional) arranque real ✔.

### FASE 7 — Cobertura y calidad
Completa tests faltantes (mappers JPA/Mongo/REST, adaptadores, controladores, servicios); revisa que **ninguna regla de R-03…R-09 quede sin test**. Ejecuta `./mvnw -q clean test`.

### FASE 8 — Cierre
1. `README.md` (en español): descripción, arquitectura, capas, cómo levantar BD, variables de entorno, cómo correr tests, tabla de endpoints, roles.
2. `SDD/Self-check-rubric.md`: autoevaluación contra los 5 criterios con **rutas y clases como evidencia** (no uses el nombre `evaluacion1.md`, R-11).
3. Verificaciones finales automáticas:
   - sin imports de frameworks en `domain/` (grep de Fase 2);
   - sin `String` abiertos en campos acotados (revisa `status|type|role|condition|movement` en `domain/models`);
   - sin `Double`/`double`/`float` monetarios;
   - sin identificadores en español en `src/` (busca términos como `comprador|vendedor|bodega|pedido|factura|envio|devolucion|reembolso` en `src/`);
   - todo elemento de `SDD/Traceability-Matrix.md` apunta a una clase existente.
4. Último `docs(progress)` y push.

---

## 8. DEFINICIÓN DE TERMINADO

- [ ] `./mvnw clean test` pasa al 100 % (JUnit 5 + Mockito), sin necesitar MySQL/Mongo.
- [ ] `domain/` sin dependencias de Spring/JPA/Mongo/Jackson/HTTP.
- [ ] Los 7 dominios funcionales y OBJ-01…OBJ-12 cubiertos por servicio + puerto + endpoint + test (matriz de trazabilidad completa).
- [ ] Todas las validaciones críticas de la especificación tienen test (stock no negativo, no reservar dañado, pedido finalizado inmutable, email/documento únicos, vendedor solo por ADMIN, un rol por usuario).
- [ ] Código Java 100 % en inglés; SDD en español; sin `evaluacion1.md`.
- [ ] `docker-compose.yml`, `.env.example` y propiedades por perfil presentes; ningún secreto en Git.
- [ ] Historial limpio: commits convencionales, atómicos, que compilan; `develop` publicada en `origin` (o informe claro de por qué no se pudo).
- [ ] `PROGRESS.md` refleja solo lo verificado, con hashes reales.

---

## 9. ASUNTOS A CONFIRMAR POR LA PERSONA DUEÑA DEL PROYECTO (no bloquean; asume la decisión y registra el supuesto)

D-04 (renombrar `ACTIVE→ELIGIBLE`) · D-09 (matriz de responsabilidades ⚠) · D-10 (6 puertos en lugar de 8) · D-11 (claim `role` informativo) · D-12 (IVA 19 % incluido) · D-08 (cancelación solo antes del pago, un envío por bodega).

---

## 10. INFORME POR FASE Y CONDICIONES DE PARADA

**Al cerrar cada fase responde con este formato (breve):**
```
FASE N — <nombre>
Estado: COMPLETA | PARCIAL | BLOQUEADA
Cambios: <resumen en 3-6 líneas>
Commits: <hash corto + asunto, uno por línea (de git log --oneline)>
Compilación/Tests: <comando> → <resultado y nº de tests>
Push: OK | FALLÓ (<motivo>) | OMITIDO
Supuestos/decisiones nuevas: <lista o "ninguno">
Pendiente / riesgos: <lista o "ninguno">
```

**Detente y pregunta si:**
- `origin` no coincide con el repositorio esperado, falta identidad Git, o hay conflicto/estado Git inesperado.
- Una dependencia no resuelve o exige cambiar la versión de Spring Boot/Java.
- Una regla de la especificación contradice este prompt de forma que no puedas resolver con la precedencia de §2.
- Un test no pasa y la corrección implicaría debilitar una regla de negocio (nunca "arregles" un test relajando una invariante).
- Detectas que el repo contiene un stack distinto a Java/Spring.

---

## ANEXO A — ESPECIFICACIÓN FUNCIONAL DE NEXUSMARKET (transcripción del PDF)
*(Guardar tal cual como `SDD/especificacion_funcional_nexusmarket.md` en la Fase 1.)*

### A.1 Contexto
NexusMarket es una plataforma digital centralizada que actúa como intermediario comercial entre compradores y vendedores. Administra la operación completa: registro de usuarios, publicación de productos, logística, facturación y posventa, con trazabilidad y coordinación entre todos los participantes.

### A.2 Objetivos
| Código | Objetivo funcional |
|---|---|
| OBJ-01 | Administrar la información de todos los usuarios del Marketplace |
| OBJ-02 | Gestionar el registro y administración de vendedores |
| OBJ-03 | Administrar compradores registrados |
| OBJ-04 | Controlar la información de las bodegas |
| OBJ-05 | Gestionar el catálogo de productos |
| OBJ-06 | Administrar el inventario distribuido |
| OBJ-07 | Gestionar el carrito de compras |
| OBJ-08 | Controlar el ciclo completo de los pedidos |
| OBJ-09 | Administrar la facturación de las compras |
| OBJ-10 | Gestionar los procesos logísticos |
| OBJ-11 | Administrar devoluciones y reembolsos |
| OBJ-12 | Consolidar información administrativa para consulta |

### A.3 Alcance
**Incluido:** registro de compradores, administración de usuarios, registro administrativo de vendedores, administración de bodegas, gestión del catálogo, administración del inventario, gestión del carrito, gestión de pedidos, facturación, envíos, devoluciones, reembolsos y consulta de reportes administrativos.
**Fuera del alcance (del documento funcional):** interfaces gráficas, aplicaciones móviles, portales web, mecanismos de autenticación técnica, tecnologías de implementación, arquitectura del software y almacenamiento de la información. *(La implementación técnica se rige por el SDD y este prompt.)*

### A.4 Componentes del negocio
Usuarios (personas autorizadas) · Vendedores (comercializan productos) · Compradores (realizan compras) · Bodegas (inventario físico) · Productos (bienes físicos o digitales) · Inventario (existencias disponibles) · Pedidos (solicitudes de compra) · Facturación (información comercial de ventas) · Envíos (logística de productos físicos).

### A.5 Participantes
Cada participante desempeña **un único rol** y solo interactúa con la información de sus funciones.
| Participante | Descripción |
|---|---|
| Comprador | Adquiere productos publicados |
| Vendedor | Registra y administra sus productos |
| Operador Logístico | Operación física de bodegas y despachos |
| Administrador | Administración de vendedores y bodegas |
| Supervisor | Perfil de consulta y seguimiento operativo |

### A.6 Flujo general del negocio
1. **Incorporación:** el Administrador registra al vendedor y su primera bodega.
2. **Catálogo:** el vendedor registra productos y define sus características.
3. **Inventario:** se registran existencias iniciales en las bodegas asociadas.
4. **Publicación:** los productos se hacen visibles en el catálogo público.
5. **Compra:** el comprador selecciona productos mediante el carrito y confirma el pedido.
6. **Transacción:** se valida el pago y se inicia el flujo de preparación.
7. **Logística:** empaque, despacho y transporte del pedido.
8. **Cierre:** el pedido se marca como finalizado tras la entrega confirmada.

### A.7 Dominios funcionales
**Dominio 1 — Administración de Usuarios** (todos los roles). Atributos: Identificador (obligatorio, único) · Nombre completo (obligatorio, no vacío) · Correo electrónico (obligatorio, único; medio principal de acceso y comunicación) · Rol (obligatorio, único por usuario) · Estado (obligatorio, catálogo definido: Activo, Bloqueado, etc.). *Validación crítica: documento de identidad y correo únicos.*
**Dominio 2 — Gestión de Compradores.** Atributos: Dirección principal (obligatorio) · Direcciones adicionales (opcional) · Estado comercial (obligatorio; condición para realizar compras). *Restricción: el comprador nunca administra información de otros compradores ni inventarios.*
**Dominio 3 — Gestión de Vendedores.** Los vendedores **no pueden autorregistrarse**; los incorpora el Administrador.
**Dominio 4 — Gestión de Bodegas.** Controla espacios físicos de almacenamiento; se distinguen **bodegas del Marketplace** y **bodegas de Vendedores**.
**Dominio 5 — Gestión del Catálogo.** Productos **físicos** (requieren inventario y despacho) y **digitales** (entrega inmediata tras el pago). Atributos: Tipo de producto (Físico/Digital) · Variantes (lista: color, talla, modelo…) · Estado (Publicado, Suspendido, Descontinuado).
**Dominio 6 — Gestión del Inventario.** Distribuido; vinculado obligatoriamente a un producto y a una bodega. Movimientos: Ingreso, Reserva, Salida por venta, Ajuste y Devolución. *Restricción: no se permiten existencias negativas bajo ninguna circunstancia.*
**Dominio 7 — Gestión de Pedidos.** Compromiso comercial formal; su ciclo de vida es el proceso central. Estados: 1) Carrito (selección provisional) → 2) Pendiente de Pago → 3) Pagado (inicio de alistamiento) → 4) Despachado (salida física de bodega) → 5) Entregado/Finalizado.

### A.8 Restricciones generales
| Código | Restricción |
|---|---|
| RG-01 | Toda operación debe ejecutarse por un usuario autenticado |
| RG-02 | Cada usuario tendrá un único rol dentro del sistema |
| RG-03 | Ningún participante podrá administrar información fuera de su rol |

### A.9 Validaciones críticas
- **Inventario:** no se puede reservar inventario inexistente o marcado como "Dañado".
- **Pedidos:** un pedido finalizado no podrá ser modificado bajo ninguna circunstancia.
- **Usuarios:** el documento de identidad y el correo electrónico deben ser únicos en la plataforma.

### A.10 Matriz de responsabilidades (⚠ transcripción con columnas perdidas — ver D-09)
| Proceso | Marcas ✔ detectadas en el PDF (Comprador / Vendedor / Op. Logístico / Admin) |
|---|---|
| Registro de Vendedores | 1 marca (interpretada: Admin) |
| Registro de Productos | 1 marca (interpretada: Vendedor) |
| Administración de Inventario | 2 marcas (interpretadas: Vendedor + Op. Logístico) |
| Gestión de Pedidos | 3 marcas (interpretadas: Comprador + Vendedor + Op. Logístico) |
| Gestión de Reembolsos | 2 marcas (interpretadas: Comprador + Admin) |

---

## ANEXO B — TRADUCCIÓN DE LA PLANTILLA BANCARIA (`Orchestrator-prompt.md`) A NEXUSMARKET

| Plantilla bancaria | NexusMarket |
|---|---|
| "sistema bancario" | "marketplace NexusMarket" |
| BD MySQL `bank_db`, usuario `bank_app` | `nexusmarket_db`, `nexus_app` |
| MongoDB `audit_db` | `nexus_audit_db` (colección `audit_logs`) |
| contenedores `bank-mysql`, `bank-mongo` | `nexus-mysql`, `nexus-mongo` |
| host Docker `mysql-db` / `mongo-db` | igual (perfil `docker`); `localhost` en perfil `local` |
| `Person`, `Customer` (`NaturalCustomer`, `BusinessCustomer`), `BankingProduct`, `BankAccount`, `Loan`, `Transfer`, `Operation` | `User`, `Buyer`, `Seller`, `Warehouse`, `Product`, `Inventory`, `InventoryMovement`, `Cart`, `Order`, `Invoice`, `Shipment`, `Return`, `Refund`, `AuditLog` |
| `AccountStatus`, `LoanStatus`, `TransferStatus`, `OperationType`, `ApprovalDecision` | `UserStatus`, `CommercialStatus`, `ProductStatus`, `OrderStatus`, `PaymentStatus`, `ShipmentStatus`, `ReturnStatus`, `RefundStatus`, `MovementType`, … (D-06) |
| `CustomerRepositoryPort`, `BankAccountRepositoryPort`, `LoanRepositoryPort`, `TransferRepositoryPort`, `OperationRepositoryPort`, `AuditRepositoryPort` | puertos de salida de la Fase 2 (`AuditPort` reemplaza a `AuditRepositoryPort`) |
| `NaturalCustomerPort` / `BusinessCustomerPort` / `BusinessOperatorPort` / `BusinessSupervisorPort` / `TellerEmployeePort` | `NaturalCustomerPort` = BUYER · `BusinessCustomerPort` = SELLER · `BusinessOperatorPort` = LOGISTIC_OPERATOR · `BusinessSupervisorPort` = SUPERVISOR · `TellerEmployeePort` = ADMIN · `PublicAccessPort` = público |
| `CommercialEmployeePort`, `InternalAnalystPort` | **No aplican** (D-10) |
| `CustomerServiceTest`, `LoanServiceTest`, `TransferServiceTest` | `UserServiceTest`, `InventoryServiceTest`, `OrderServiceTest`, … |
| Auto-DDL `update` y colección creada al primer evento | se mantiene |

---
*Fin del prompt maestro. Empieza por la Fase 0 y reporta al cerrarla.*
