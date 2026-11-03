# `UserService`

**OBJ:** OBJ-01 · **Acceso:** `ADMIN` (excepto consulta del propio perfil)
**Paquete:** `application.domain.services.UserService`

## Dependencias (constructor)

`UserRepositoryPort`, `PasswordServicePort`, `AuditPort`, `ClockPort`.

## Métodos

### `User createUser(User actor, FullName fullName, Email email, IdentityDocument identityDocument, UserRole role, String rawPassword)`

- **Precondiciones:** actor `ACTIVE` y `ADMIN` (`InsufficientPermissionException`).
- **Flujo:** valida duplicados (`DuplicateEmailException`,
  `DuplicateIdentificationException`), codifica la contraseña, crea el `User`
  `ACTIVE` con el rol indicado y guarda.
- **Auditoría:** `USER_CREATED` / `USER`.

### `List<User> listUsers(User actor)`

- Actor `ADMIN`; devuelve todos los usuarios. Sin auditoría.

### `User getUser(User actor, String userId)`

- Permite `ADMIN` o el propio usuario (`actor.id.equals(userId)`); si no ⇒
  `UnauthorizedOperationException`. Si no existe ⇒ `UserNotFoundException`.

### `User updateUserProfile(User actor, String userId, FullName fullName, Email email)`

- `ADMIN` o el propio usuario. Verifica unicidad del nuevo correo
  (`DuplicateEmailException`), actualiza nombre y correo mediante métodos de
  negocio. Sin auditoría específica (no está en D-14).

### `User blockUser(User actor, String userId)` / `activateUser` / `deactivateUser`

- Solo `ADMIN`. No permite que un administrador se bloquee o desactive a sí
  mismo (`UnauthorizedOperationException`).
- **Flujo:** carga el usuario (`UserNotFoundException`), aplica
  `block()`, `activate()` o `deactivate()` (incrementa `authTokenVersion`) y
  guarda.
- **Auditoría:** `USER_BLOCKED` / `USER_ACTIVATED` / `USER_DEACTIVATED` sobre
  `USER`.

## Reglas

- Ningún método cambia el rol de un usuario (RG-02).
- El correo y el documento son únicos (validación doble: servicio + BD).
- Un usuario `BLOCKED` o `INACTIVE` no puede operar (lo garantiza
  `assertCanOperate` en cualquier servicio).
