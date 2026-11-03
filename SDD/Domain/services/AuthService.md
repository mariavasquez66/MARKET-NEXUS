# `AuthService`

**OBJ:** OBJ-01 (usuarios), OBJ-03 (compradores) · **Acceso:** público
**Paquete:** `application.domain.services.AuthService`

## Dependencias (constructor)

`UserRepositoryPort`, `BuyerRepositoryPort`, `PasswordServicePort`,
`JwtServicePort`, `AuditPort`, `ClockPort`.

## Métodos

### `AuthResult login(String email, String rawPassword)`

| Elemento | Detalle |
|---|---|
| Precondiciones | `email` y `rawPassword` no vacíos |
| Flujo | 1) `Email.of(email)` (normaliza). 2) `userRepository.findByEmail`; si vacío ⇒ `UnauthenticatedUserException("Invalid credentials")`. 3) `passwordService.matches(rawPassword, user.passwordHash)`; si falla ⇒ misma excepción. 4) `user.assertCanOperate()` (`InactiveUserException`). 5) `token = jwtService.issueToken(user)`. 6) devuelve `AuthResult(token, user)`. |
| Excepciones | `InvalidDataException`, `UnauthenticatedUserException`, `InactiveUserException` |
| Auditoría | Ninguna (login no es una mutación de negocio) |

### `User registerBuyer(FullName fullName, Email email, IdentityDocument identityDocument, String rawPassword, Address mainAddress)`

| Elemento | Detalle |
|---|---|
| Precondiciones | `fullName`, `email`, `identityDocument`, `mainAddress` válidos; `rawPassword` no vacío |
| Flujo | 1) Verifica `userRepository.existsByEmail(email)` ⇒ `DuplicateEmailException`. 2) `existsByIdentityDocument` ⇒ `DuplicateIdentificationException`. 3) `passwordHash = passwordService.encode(rawPassword)`. 4) `User.create(..., UserRole.BUYER, passwordHash)`. 5) `Buyer.create(user, mainAddress)` con `CommercialStatus.ELIGIBLE`. 6) Guarda usuario y comprador. 7) Audita `USER_CREATED` sobre `USER`. |
| Excepciones | `DuplicateEmailException`, `DuplicateIdentificationException`, `InvalidDataException` |
| Auditoría | `AuditAction.USER_CREATED`, `AuditEntityType.USER`, entidad = id del usuario, actor = el propio usuario recién creado |

## Reglas

- Nunca devuelve `passwordHash` ni el hash en el token.
- El rol del comprador es siempre `BUYER`; no se acepta rol por parámetro.
- El login no revela si el correo existe (mismo mensaje para credenciales
  inválidas).
