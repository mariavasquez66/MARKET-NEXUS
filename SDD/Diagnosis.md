# Diagnóstico del repositorio — Fase 0

**Fecha:** 2026-11-01 · **Rama:** `develop` (creada desde `main`) · **Remoto:** `origin = https://github.com/mariavasquez66/MARKET-NEXUS.git` (correcto)

## 1. Estado Git inicial

- Rama inicial `main`, con **1 commit local sin publicar** respecto a `origin/main`:
  `b29f780 mvgit`.
- Historial real (5 commits): `b29f780 mvgit`, `81aa717 sdd`, `3353424 mvgit`,
  `5ddea94 arg`, `f40fdbe Initial commit`. Los hashes citados en el antiguo
  `PROGRESS.md` (`8e9ad2c`, `45056dd`, …) **no existen** (H-02).
- El working tree mostraba **tres archivos de `SDD/` borrados** que sí existen en
  `HEAD`: `Orchestrator-prompt.md`, `Prompt NexusMarket.md`,
  `enunciado evaluativo.md`. Se restauraron con `git restore` (no se modificó su
  contenido).
- Archivos no versionados presentes: el PDF de la especificación y
  `SDD/OpenCode-master-prompt.md`.
- `.gitignore` no cubría `.env` ni `logs/`; se añadieron. `.vscode/` estaba
  ignorado por completo; ahora se comparte solo `.vscode/settings.json`.
- Identidad Git configurada (`mariavasquez66` / correo institucional).

## 2. Entorno de compilación

- Solo había **JRE 8** instalado; `javac` no existía y `mvn compile` fallaba con
  *"No compiler is provided in this environment"*.
- Se instaló **Eclipse Temurin JDK 17.0.20.1** mediante `winget`
  (`EclipseAdoptium.Temurin.17.JDK`). Ruta:
  `C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot`.
- Maven Wrapper 3.3.4, `distributionType=only-script` (no requiere el JAR del
  wrapper); descarga Maven 3.9.16.
- **Spring Boot 4.1.1 resuelve correctamente**; no fue necesario cambiar
  versiones.

## 3. Verificación de hallazgos H-01…H-10

| ID | Hallazgo | Resultado de la verificación |
|---|---|---|
| H-01 | Código mínimo (10 enums + esqueleto vacío) | **Confirmado**. `src/main/java/application/domain/enums` contiene `DomainCatalog`, `UserRole`, `UserStatus`, `CommercialStatus`, `ProductType`, `ProductStatus`, `OrderStatus`, `PaymentStatus`, `ShipmentStatus`, `ReturnStatus`. El resto de paquetes existe pero está vacío. |
| H-02 | `PROGRESS.md` inflado | **Confirmado**. Afirmaba entidades, 13 servicios, puertos, excepciones y 7 tests; nada de eso existe. Se reescribió con el estado verificado. |
| H-03 | Enums no compilan | **Confirmado**. Los 10 `.java` tenían BOM UTF-8 (U+FEFF) y el error de `javac` era `illegal character: '\ufeff'`; además importaban `InvalidDataException`, inexistente. Se eliminó el BOM y se creó la jerarquía mínima `DomainException` → `InvalidDataException`. |
| H-04 | Archivos vacíos por mala extracción | **No aplica**. `Get-ChildItem -Recurse -File src | Where Length -eq 0` no devolvió resultados. |
| H-05 | `SDD/` incompleta | **Confirmado**. Solo existen los prompts y el PDF; las carpetas `SDD/Adapters`, `SDD/C4`, `SDD/domain` y `SDD/Software Architecture` están vacías. No hay modelo de dominio ni matriz de trazabilidad. |
| H-06 | Inconsistencias entre fuentes | **Confirmado** a nivel documental; se resolverán con las decisiones D-01…D-17 al escribir el SDD (Fase 1). |
| H-07 | Catálogos tipo clase, no `enum` | **Confirmado**. Solo `UserRole` implementa `values()`; el resto no. `CommercialStatus` sigue con `ACTIVE/RESTRICTED` (falta renombrar a `ELIGIBLE` y añadir `SUSPENDED`, D-04). |
| H-08 | `pom.xml` incompleto | **Parcialmente corregido**. `name=nexusmarket`, `description` y eliminación de bloques vacíos. Faltan `spring-boot-starter-validation` y la librería JWT (Fase 4). |
| H-09 | PDF no legible por el agente | **Resuelto**. Extraído con PDFBox 2.0.31; 8 páginas verificadas por texto y coordenadas. |
| H-10 | Matriz de responsabilidades desalineada | **Resuelto**. D-09 confirmada contra PDF p.8 §12: las 4 columnas existen; solo filas/roles ausentes quedan como supuesto. |

## 4. Otras observaciones

- `README.md` contiene texto con codificación dañada (mojibake) y una sola línea;
  se reescribirá en la Fase 8.
- `.vscode/settings.json` incluía una referencia heredada a
  `BankApplicationTests` (plantilla bancaria); se reemplazó por configuración de
  codificación UTF-8 sin BOM y análisis nulo de Java.
- `src/main/resources` y `src/test/java/application/domain` están vacíos.
- No existe `docker-compose.yml`, `.env.example` ni perfiles de Spring.
- `target/` está correctamente ignorado; no se versionó ningún artefacto.

## 5. Correcciones aplicadas en la Fase 0

1. BOM eliminado de los 10 enums (UTF-8 sin BOM).
2. Creadas `DomainException` e `InvalidDataException` en
   `application.domain.exceptions.common`.
3. `pom.xml`: metadatos completados y bloques vacíos retirados.
4. `.gitignore`: `.env`, `logs/` y excepción para `.vscode/settings.json`.
5. `.vscode/settings.json`: UTF-8 sin BOM, sin referencias a la plantilla
   bancaria.
6. `PROGRESS.md` reescrito con estado verificado y hashes reales.

## 6. Supuestos y decisiones de la fase

- Se instaló el JDK 17 en el sistema con autorización de la persona dueña del
  proyecto; no se modificó la versión de Java ni la de Spring Boot del `pom.xml`.
- El primer commit de la fase agrupa la eliminación del BOM y la creación de la
  jerarquía de excepciones porque **ninguno de los dos cambios compila por
  separado** (los enums referencian `InvalidDataException` y el BOM rompe
  `javac`). El resto de commits sí son atómicos.
- No se creó ningún archivo `evaluacion1.md` (R-11).

## 7. Puerta de la Fase 0

```
.\mvnw.cmd -q -DskipTests compile  →  EXIT=0
```

Los 10 catálogos y las 2 excepciones compilan con Java 17.
