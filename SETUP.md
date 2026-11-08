# NexusMarket setup

## Prerequisites

- Java 17
- Docker (for MySQL, MongoDB, and the optional app container)
- Maven Wrapper (`mvnw` / `mvnw.cmd`) is included

## Local development

1. Copy `.env.example` to `.env` and set the required secrets.
2. Start the databases:

   ```bash
   docker compose up -d mysql-db mongo-db
   ```

3. Run the application with the local profile:

   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
   ```

   On Windows PowerShell:

   ```powershell
   .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
   ```

4. The API is available at `http://localhost:8080`. Health is exposed at
   `GET /actuator/health`; Swagger UI is available outside the `prod` profile.

## Docker Compose

`docker compose up --build` starts MySQL, MongoDB, and the application. The app
container runs as a non-root user and depends on both databases being healthy.
Set `MYSQL_PASSWORD`, `MYSQL_ROOT_PASSWORD`, `MONGO_ROOT_USERNAME`,
`MONGO_ROOT_PASSWORD`, and at least a 32-byte `JWT_SECRET` in `.env`.

## Tests

```bash
./mvnw clean test
```

Integration tests use an in-memory H2 database. Unit tests use the existing
copying fakes to reproduce persistence round-trips without a database.

## Bootstrap administrator

Set `NEXUS_BOOTSTRAP_ADMIN_EMAIL` and `NEXUS_BOOTSTRAP_ADMIN_PASSWORD` before
the first start. Bootstrap is skipped when an administrator already exists or
the variables are missing. The password must contain 8-72 characters with at
least one letter and one number.

## Production profile

Run with `SPRING_PROFILES_ACTIVE=prod`. In production, Swagger/OpenAPI is
disabled, only the health actuator endpoint is exposed, and secrets must come
from environment variables.
