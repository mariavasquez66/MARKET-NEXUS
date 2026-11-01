# NexusMarket — Estado de implementación verificado

> Este documento refleja **solo lo comprobado** en el sistema de archivos y en Git.
> No es fuente de verdad del diseño: el SDD lo será a partir de la Fase 1.
> Última actualización: Fase 0 (rama `develop`).

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

- `src/main/java/application/domain/enums/` — 10 catálogos `DomainCatalog`:
  `DomainCatalog`, `UserRole`, `UserStatus`, `CommercialStatus`, `ProductType`,
  `ProductStatus`, `OrderStatus`, `PaymentStatus`, `ShipmentStatus`, `ReturnStatus`.
- `src/main/java/application/domain/exceptions/common/` — `DomainException` (abstracta,
  `RuntimeException`) e `InvalidDataException`.
- Esqueleto de paquetes vacíos: `adapters/{persistence/{jpa,mongodb},rest,useCases}`,
  `domain/{exceptions,models,ports/{in,out},services,valueobjects}`,
  `infrastructure/{config,exception,security}`.

### No existe todavía

- `domain/models`, `domain/valueobjects`, `domain/services`, puertos, adaptadores,
  DTOs, controladores, configuración, seguridad y pruebas automatizadas.
- `src/test/java/application/domain` está vacío: **no hay tests** (el antiguo
  `PROGRESS.md` afirmaba 7 tests y 13 servicios; no era cierto).
- Documentación de dominio en `SDD/` (solo hay prompts y el PDF).

### Verificaciones de la Fase 0

| Hallazgo | Resultado |
|---|---|
| H-01 código mínimo | Confirmado (10 enums + esqueleto vacío) |
| H-02 `PROGRESS.md` inflado | Confirmado; este documento lo sustituye |
| H-03 enums con BOM y sin `InvalidDataException` | Corregido; `compile` pasa |
| H-04 archivos vacíos | No se encontraron archivos de 0 bytes en `src/` |
| H-05 `SDD/` incompleta | Confirmado; se completará en Fase 1 |
| H-06 inconsistencias entre fuentes | Pendiente de resolver documentalmente en Fase 1 |
| H-07 `DomainCatalog` sin `values()` | Confirmado (solo `UserRole` lo tiene) |
| H-08 `pom.xml` incompleto | Metadatos corregidos; faltan validation/JWT (Fase 4) |
| H-09 PDF no legible | Se transcribirá como Anexo A en Fase 1 |
| H-10 matriz de responsabilidades | Pendiente de confirmación (D-09) |

## Fases

| Fase | Estado | Evidencia |
|---|---|---|
| 0 — Diagnóstico y línea base | COMPLETA | `SDD/Diagnosis.md`, compilación OK |
| 1 — SDD completo | PENDIENTE | — |
| 2 — Dominio core | PENDIENTE | — |
| 3 — Servicios y casos de uso | PENDIENTE | — |
| 4 — Persistencia JPA/Mongo | PENDIENTE | — |
| 5 — REST | PENDIENTE | — |
| 6 — Seguridad e infraestructura | PENDIENTE | — |
| 7 — Cobertura y calidad | PENDIENTE | — |
| 8 — Cierre | PENDIENTE | — |

## Historial real de commits

```
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
.\mvnw.cmd -q test                  # sin tests todavía
```
