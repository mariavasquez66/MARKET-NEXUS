# Security

## Secrets

- Never commit `.env`, database passwords, or JWT secrets.
- `JWT_SECRET` must contain at least 32 bytes. The application fails fast when
  it is missing or shorter.
- Use distinct database credentials per environment and rotate them regularly.

## Authentication

- Passwords are hashed with BCrypt and are never returned by the REST API.
- Passwords must contain 8-72 characters, including at least one letter and one
  number.
- JWTs include the user's role and token version. Logout and password changes
  increment the token version, immediately invalidating previously issued
  tokens.
- Authentication events are written to the audit trail.

## Authorization

- Endpoints are scoped by role: buyer, seller, logistics operator, supervisor,
  and administrator.
- Buyer-owned resources are checked against the authenticated user before reads
  or writes.
- Data integrity violations are mapped to HTTP 409 without leaking persistence
  details.

## Production

- Set `SPRING_PROFILES_ACTIVE=prod`.
- Swagger/OpenAPI is disabled in the `prod` profile.
- Only `/actuator/health` is exposed, and health details are hidden.
- Run the container as the non-root `nexus` user.
- Configure TLS at the ingress or reverse proxy and restrict CORS with
  `CORS_ALLOWED_ORIGINS`.

## Reporting

Report vulnerabilities privately to the maintainers. Do not open a public issue
for an unpatched security problem.
