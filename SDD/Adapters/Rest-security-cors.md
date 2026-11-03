# Seguridad REST: JWT, autorización, CORS y `X-Request-Id`

> Paquete: `application.infrastructure.security` (JWT/filtros) y
> `application.adapters.rest` (entry points). Implementación en Fase 6.

## 1. JWT (D-11)

Claims emitidos por `JwtProvider.issueToken(User)`:

| Claim | Contenido |
|---|---|
| `sub` | `User.id` (inmutable) |
| `jti` | UUID del token |
| `ver` | `User.authTokenVersion` |
| `iat` | emisión (`ClockPort.now()`) |
| `exp` | `iat + jwt.expiration-ms` (por defecto 1 h) |
| `role` | `User.role.getCode()` (**informativo**) |

- **Sin PII**: no se incluyen nombre, correo, documento, dirección ni snapshot de
  permisos.
- Algoritmo y secreto: `JWT_SECRET` por variable de entorno (nunca en Git).
- El `role` del token **no autoriza por sí solo**: el filtro lo compara con el
  rol actual del usuario en base de datos.

## 2. Flujo del filtro

`JwtAuthenticationFilter extends OncePerRequestFilter`:

1. Si la ruta es `/api/v1/public/**` o el preflight `OPTIONS`, continúa sin
   token.
2. Lee `Authorization: Bearer <token>`; si falta en ruta protegida ⇒ 401.
3. `JwtProvider` valida firma, `exp` y estructura; token inválido/expirado ⇒ 401
   (`INVALID_TOKEN`).
4. Extrae `sub` y `ver`; carga `UserRepositoryPort.findById(sub)`:
   - inexistente ⇒ 401,
   - `status != ACTIVE` ⇒ 401,
   - `user.authTokenVersion != ver` ⇒ 401 (token obsoleto),
   - `role` del claim distinto del rol actual ⇒ 401.
5. Construye `AuthenticatedUserPrincipal(user)` y lo coloca en el
   `SecurityContext`. El controlador recibe el `User` de dominio vigente
   (`@AuthenticationPrincipal`).
6. Si el token es válido, añade `X-Request-Id` (o genera uno) y continúa.

`SecurityConfig`:

```java
@Bean
SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http
        .csrf(csrf -> csrf.disable())
        .sessionManagement(sm -> sm.sessionCreationPolicy(STATELESS))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/v1/public/**").permitAll()
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            .requestMatchers("/api/v1/buyer/**").hasRole("BUYER")
            .requestMatchers("/api/v1/seller/**").hasRole("SELLER")
            .requestMatchers("/api/v1/logistics/**").hasRole("LOGISTIC_OPERATOR")
            .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
            .requestMatchers("/api/v1/supervisor/**").hasRole("SUPERVISOR")
            .anyRequest().authenticated())
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint(custom401())
            .accessDeniedHandler(custom403()))
        .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
        .build();
}
```

- Sin sesión (`STATELESS`), sin formulario, sin `httpBasic`.
- `PasswordEncoder` = `BCryptPasswordEncoder`; `PasswordServicePort` lo envuelve.
- `SUPERVISOR` solo tiene endpoints `GET`; cualquier otro método ⇒ 403.

## 3. Matriz ruta ↔ rol

| Prefijo | Rol exigido |
|---|---|
| `/api/v1/public/**` | ninguno (abierto) |
| `/api/v1/buyer/**` | `BUYER` |
| `/api/v1/seller/**` | `SELLER` |
| `/api/v1/logistics/**` | `LOGISTIC_OPERATOR` |
| `/api/v1/admin/**` | `ADMIN` |
| `/api/v1/supervisor/**` | `SUPERVISOR` |

La autorización fina por recurso (propiedad) se hace en los servicios de
dominio (`UnauthorizedOperationException` ⇒ 403).

## 4. CORS

`CorsConfigurationSource` con orígenes configurables por propiedad
(`nexusmarket.cors.allowed-origins`, por ejemplo `http://localhost:3000`):

- Métodos: `GET, POST, PUT, DELETE, OPTIONS`.
- Cabeceras permitidas: `Authorization`, `Content-Type`, `X-Request-Id`.
- Cabeceras expuestas: `X-Request-Id`.
- `allowCredentials(true)` solo si los orígenes son explícitos (nunca `*`).
- Preflight `OPTIONS` respondido con 200 sin exigir token.

## 5. `X-Request-Id`

`RequestIdFilter` (orden alto, antes de seguridad):

1. Lee `X-Request-Id`; si falta o es inválido (≤ 100 caracteres), genera un UUID.
2. Lo guarda en `request.setAttribute("requestId", ...)`.
3. Lo devuelve en la respuesta (`response.setHeader("X-Request-Id", ...)`).
4. `GlobalExceptionHandler` lo incluye en el cuerpo de error.

## 6. Bootstrap del administrador (D-16)

`BootstrapAdminRunner implements CommandLineRunner` en
`infrastructure.config`:

- Si `UserRepositoryPort.existsByRole(UserRole.ADMIN)` es `false`, lee
  `NEXUS_BOOTSTRAP_ADMIN_EMAIL` y `NEXUS_BOOTSTRAP_ADMIN_PASSWORD`.
- Si ambas existen, crea `User` `ADMIN` con `UserService`/`AuthService` y
  registra el evento; si faltan, no crea nada y escribe un `WARN`.
- No se ejecuta en tests (`@Profile("!test")`).

## 7. Propiedades

```properties
jwt.secret=${JWT_SECRET}
jwt.expiration-ms=${JWT_EXPIRATION_MS:3600000}
nexusmarket.tax.rate=${NEXUS_TAX_RATE:0.19}
nexusmarket.cors.allowed-origins=${CORS_ALLOWED_ORIGINS:http://localhost:3000}
```

## 8. Pruebas previstas (Fase 6)

- 401 sin token.
- 401 con token inválido, expirado, con `ver` obsoleto, con rol alterado o con
  usuario `BLOCKED`/`INACTIVE`.
- 403 con rol incorrecto en cada prefijo (buyer/seller/logistics/admin/supervisor).
- Preflight `OPTIONS` 200 y cabeceras CORS correctas.
- Presencia de `X-Request-Id` en respuestas exitosas y de error.
