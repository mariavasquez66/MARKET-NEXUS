# NexusMarket

NexusMarket es una plataforma digital centralizada que actúa como intermediario comercial entre compradores y vendedores. Administra el registro de usuarios, el catálogo, el inventario distribuido, el carrito, los pedidos, la facturación, la logística y la posventa, manteniendo trazabilidad de las operaciones mediante auditoría.

El proyecto está implementado con **Java 17**, **Spring Boot 4.1.1**, **arquitectura hexagonal** y **DDD**. El dominio es puro: no depende de Spring, JPA, MongoDB ni HTTP.

## Arquitectura

```text
src/main/java/application/
├── domain/                  Dominio puro
│   ├── models/              Agregados, entidades y modelos de reporte
│   ├── valueobjects/        Money, Email, Address, TaxId, etc.
│   ├── enums/               Catálogos acotados basados en DomainCatalog
│   ├── services/            Reglas de negocio y servicios de dominio
│   ├── ports/in/            Puertos de entrada por rol
│   ├── ports/out/           Puertos de salida
│   └── exceptions/          Excepciones de dominio
├── adapters/
│   ├── useCases/            Implementaciones de los puertos de entrada
│   ├── rest/                Controladores, DTOs y mappers REST
│   └── persistence/
│       ├── jpa/             Entidades, converters, mappers y adaptadores JPA
│       └── mongodb/         Documento, mapper, repositorio y adaptador de auditoría
└── infrastructure/
    ├── config/              Cableado de servicios, impuestos y bootstrap
    ├── security/            JWT, filtro, reglas por rol, CORS y request id
    └── exception/           Manejo global de errores
```

Flujo de dependencias:

```text
REST / Use cases -> Puertos de entrada -> Servicios de dominio -> Puertos de salida -> JPA / MongoDB / Seguridad
```

### Capas

| Capa | Responsabilidad | Evidencia |
|---|---|---|
| Dominio | Entidades ricas, Value Objects, catálogos, invariantes y servicios puros | `src/main/java/application/domain/` |
| Casos de uso | Adaptan los puertos de entrada y delegan en servicios de dominio | `src/main/java/application/adapters/useCases/` |
| REST | Validación de entrada, DTOs, mappers y respuestas HTTP | `src/main/java/application/adapters/rest/` |
| Persistencia | Adaptadores JPA para MySQL y adaptador Mongo para auditoría | `src/main/java/application/adapters/persistence/` |
| Infraestructura | Seguridad JWT, configuración, CORS y manejo global de errores | `src/main/java/application/infrastructure/` |

## Requisitos

- Java 17
- Docker Desktop o Docker Engine con Docker Compose
- No es necesario instalar Maven: se usa `mvnw` / `mvnw.cmd`

## Bases de datos

| Base | Uso | Configuración |
|---|---|---|
| MySQL 8.0 | Datos transaccionales (`nexusmarket_db`) | Perfil `local`: `localhost:3306`; perfil `docker`: `mysql-db:3306` |
| MongoDB 4.4 | Auditoría (`nexus_audit_db.audit_logs`) | Perfil `local`: `localhost:27017`; perfil `docker`: `mongo-db:27017` |

MongoDB se fija en **4.4** porque el host de verificación no soporta AVX; las imágenes 5.0 o superiores terminan con exit code 132. La decisión está documentada como D-17 en `SDD/Decisiones-y-supuestos.md`.

## Variables de entorno

Crea el archivo local a partir del ejemplo:

```powershell
Copy-Item .env.example .env
```

Completa `.env` con valores locales. **No versiones `.env` ni secretos reales.**

| Variable | Uso |
|---|---|
| `DB_URL` | URL JDBC de MySQL |
| `DB_USERNAME` | Usuario de la aplicación en MySQL |
| `DB_PASSWORD` | Contraseña de la aplicación en MySQL |
| `MYSQL_ROOT_PASSWORD` | Contraseña root de MySQL para Docker |
| `MYSQL_PASSWORD` | Contraseña del usuario de MySQL para Docker |
| `MONGODB_URI` | URI de conexión a MongoDB, incluida la base de auditoría |
| `MONGO_ROOT_USERNAME` | Usuario root de MongoDB para Docker |
| `MONGO_ROOT_PASSWORD` | Contraseña root de MongoDB para Docker |
| `JWT_SECRET` | Secreto de firma JWT, de al menos 32 bytes |
| `JWT_EXPIRATION_MS` | Vigencia del token en milisegundos |
| `NEXUS_BOOTSTRAP_ADMIN_EMAIL` | Correo del administrador inicial |
| `NEXUS_BOOTSTRAP_ADMIN_PASSWORD` | Contraseña del administrador inicial |
| `NEXUS_TAX_RATE` | Tasa de impuesto usada en facturación, por defecto `0.19` |
| `CORS_ALLOWED_ORIGINS` | Orígenes permitidos por CORS |

`.env.example` contiene únicamente valores de ejemplo.

## Puesta en marcha

1. Crear `.env` y levantar las bases:

```powershell
Copy-Item .env.example .env
docker compose --env-file .env up -d
docker compose ps
```

2. Cargar las variables en la sesión de PowerShell:

```powershell
Get-Content .env |
  Where-Object { $_ -match '^[A-Za-z_][A-Za-z0-9_]*=' } |
  ForEach-Object {
    $name, $value = $_ -split '=', 2
    Set-Item -Path "Env:$name" -Value $value
  }
```

3. Ejecutar la aplicación:

```powershell
.\mvnw.cmd spring-boot:run
```

La API queda disponible en `http://localhost:8080/api/v1`.

El perfil por defecto es `local`. Para usar los nombres de host de Docker se puede activar el perfil `docker`, por ejemplo:

```powershell
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=docker
```

### Administrador inicial

Si no existe ningún usuario `ADMIN`, `BootstrapAdminRunner` crea uno usando `NEXUS_BOOTSTRAP_ADMIN_EMAIL` y `NEXUS_BOOTSTRAP_ADMIN_PASSWORD`. Si estas variables no están definidas, no se crea el administrador y se registra un aviso.

## Uso de la API

1. Autenticarse con `POST /api/v1/public/auth/login`.
2. Enviar el token en las rutas protegidas:

```http
Authorization: Bearer <token>
Content-Type: application/json
```

El login devuelve `{ "token": "...", "user": { ... } }`. El token no contiene PII y los permisos se validan contra el usuario vigente en base de datos.

## Endpoints

Base: `/api/v1`. Todas las rutas, excepto `/public/**`, requieren JWT y rol autorizado.

### Público

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/public/products` | Listar productos publicados |
| GET | `/public/products/{id}` | Consultar producto publicado |
| POST | `/public/buyers/register` | Registrar comprador |
| POST | `/public/auth/login` | Iniciar sesión y obtener JWT |

### Comprador (`BUYER`)

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/buyer/cart` | Obtener carrito |
| POST | `/buyer/cart/items` | Agregar producto al carrito |
| PUT | `/buyer/cart/items/{productId}` | Actualizar cantidad |
| DELETE | `/buyer/cart/items/{productId}` | Quitar producto |
| DELETE | `/buyer/cart` | Vaciar carrito |
| POST | `/buyer/orders` | Crear pedido desde el carrito |
| GET | `/buyer/orders` | Listar pedidos propios |
| GET | `/buyer/orders/{id}` | Consultar pedido propio |
| POST | `/buyer/orders/{id}/pay` | Confirmar pago |
| POST | `/buyer/orders/{id}/cancel` | Cancelar pedido antes del pago |
| GET | `/buyer/orders/{id}/invoice` | Consultar factura |
| POST | `/buyer/orders/{id}/returns` | Solicitar devolución |
| GET | `/buyer/returns` | Listar devoluciones propias |
| GET | `/buyer/refunds` | Listar reembolsos propios |
| POST | `/buyer/addresses` | Agregar dirección |
| PUT | `/buyer/addresses/main` | Cambiar dirección principal |

### Vendedor (`SELLER`)

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/seller/products` | Crear producto |
| GET | `/seller/products` | Listar productos propios |
| PUT | `/seller/products/{id}` | Actualizar producto |
| POST | `/seller/products/{id}/variants` | Agregar variante |
| POST | `/seller/products/{id}/publish` | Publicar producto |
| POST | `/seller/products/{id}/suspend` | Suspender producto |
| POST | `/seller/products/{id}/discontinue` | Descontinuar producto |
| POST | `/seller/inventory` | Registrar inventario inicial |
| GET | `/seller/inventory` | Consultar inventario propio |
| GET | `/seller/orders` | Consultar pedidos con productos propios |

### Operador logístico (`LOGISTIC_OPERATOR`)

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/logistics/orders` | Consultar pedidos para despacho |
| POST | `/logistics/shipments` | Crear envío |
| POST | `/logistics/shipments/{id}/start` | Marcar envío en tránsito |
| POST | `/logistics/shipments/{id}/deliver` | Confirmar entrega |
| POST | `/logistics/inventory/{productId}/{warehouseId}/inflow` | Registrar entrada de inventario |
| POST | `/logistics/inventory/{productId}/{warehouseId}/adjust` | Ajustar inventario |
| POST | `/logistics/inventory/{productId}/{warehouseId}/damaged` | Marcar inventario dañado |
| POST | `/logistics/inventory/{productId}/{warehouseId}/repaired` | Marcar inventario reparado |
| GET | `/logistics/returns` | Listar devoluciones recibidas |
| POST | `/logistics/returns/{id}/receive` | Recibir devolución |

### Administrador (`ADMIN`)

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/admin/sellers` | Registrar vendedor |
| GET | `/admin/sellers` | Listar vendedores |
| PUT | `/admin/sellers/{id}` | Actualizar vendedor |
| POST | `/admin/warehouses` | Crear bodega |
| GET | `/admin/warehouses` | Listar bodegas |
| PUT | `/admin/warehouses/{id}` | Actualizar bodega |
| POST | `/admin/sellers/{id}/warehouses/{warehouseId}` | Asignar bodega a vendedor |
| GET | `/admin/users` | Listar usuarios |
| POST | `/admin/users` | Crear usuario |
| POST | `/admin/users/{id}/block` | Bloquear usuario |
| POST | `/admin/users/{id}/activate` | Activar usuario |
| POST | `/admin/users/{id}/deactivate` | Desactivar usuario |
| GET | `/admin/returns` | Listar devoluciones |
| POST | `/admin/returns/{id}/approve` | Aprobar devolución |
| POST | `/admin/returns/{id}/reject` | Rechazar devolución |
| POST | `/admin/returns/{id}/refunds` | Generar reembolso |
| POST | `/admin/refunds/{id}/process` | Procesar reembolso |

### Supervisor (`SUPERVISOR`)

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/supervisor/reports/sales` | Reporte de ventas por rango de fechas |
| GET | `/supervisor/reports/orders` | Resumen de pedidos |
| GET | `/supervisor/reports/inventory` | Resumen de inventario |
| GET | `/supervisor/reports/returns` | Resumen de devoluciones |
| GET | `/supervisor/reports/users` | Resumen de usuarios |
| GET | `/supervisor/orders` | Listar pedidos |
| GET | `/supervisor/audit-logs` | Consultar auditoría |

El contrato detallado, códigos HTTP y DTOs están en `SDD/Adapters/Api-rest-endpoints.md`.

## Roles

| Rol | Alcance |
|---|---|
| `ADMIN` | Usuarios, vendedores, bodegas, devoluciones y reembolsos |
| `BUYER` | Carrito, pedidos, direcciones, devoluciones y consulta de reembolsos |
| `SELLER` | Catálogo, inventario inicial y consulta de pedidos propios |
| `LOGISTIC_OPERATOR` | Inventario operativo, envíos y recepción de devoluciones |
| `SUPERVISOR` | Reportes y auditoría, solo lectura |

Cada usuario tiene un único rol. Los vendedores solo pueden ser creados por un administrador.

## Pruebas

```powershell
.\mvnw.cmd clean test
```

La suite usa JUnit 5 y Mockito y no necesita MySQL ni MongoDB. Al cierre de la Fase 8 se verificaron **359 pruebas**, con 0 fallos, 0 errores y 0 omitidas.

Cobertura funcional de la suite:

- Value Objects, catálogos, agregados y servicios de dominio.
- Mappers, converters, restricciones y adaptadores JPA/Mongo.
- Mappers REST, controladores, validación y manejo global de errores.
- JWT, autorización por rol, CORS, `X-Request-Id` y bootstrap del administrador.

## Documentación

- `SDD/especificacion_funcional_nexusmarket.md`: requisitos funcionales.
- `SDD/Decisiones-y-supuestos.md`: decisiones D-01 a D-17 y supuestos.
- `SDD/Domain/`: modelo de dominio, Value Objects, servicios, puertos y excepciones.
- `SDD/Adapters/`: contratos REST y de persistencia.
- `SDD/Traceability-Matrix.md`: trazabilidad requisito → artefacto → prueba.
- `SDD/Self-check-rubric.md`: autoevaluación contra la rúbrica de 5 criterios.
- `PROGRESS.md`: estado verificado, comandos y hashes de commits.
