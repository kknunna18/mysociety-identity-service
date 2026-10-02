# MySociety Identity Service

Spring Boot backend for MySociety authentication, user administration, role
assignments, and society membership.

## Features

- JWT login, refresh-token rotation, logout, and current-user retrieval
- User invitations, user listing/detail, and account status management
- Role listing and society-role assignment/revocation
- PostgreSQL schema validation against the existing `mysociety` schema
- JWT-protected APIs, Bean Validation, RFC 9457 error responses, audit events,
  OpenAPI, and Actuator health checks

## Technology

Java 21, Gradle, Spring Boot 3.4, Spring Web, Spring Security OAuth2 JOSE,
Spring Data JPA/Hibernate, PostgreSQL, MapStruct, Springdoc OpenAPI, Actuator,
JUnit 5, Mockito, AssertJ, MockMvc, Testcontainers, and ArchUnit.

## Run locally

Prerequisites: Java 21 and an existing PostgreSQL `mysociety` database/schema.

```powershell
$env:DB_USERNAME = "postgres"
$env:DB_PASSWORD = "your-password"
$env:IDENTITY_JWT_SECRET = "a-unique-secret-with-at-least-32-bytes"
.\gradlew.bat clean test
.\gradlew.bat bootRun --args="--spring.profiles.active=dev"
```

The API base URL is `http://localhost:8081/api/v1`. OpenAPI is available at
`http://localhost:8081/api/v1/swagger-ui.html`; health is available at
`http://localhost:8081/api/v1/actuator/health`.

## Database schema

Apply `database/migrations/V1__identity_schema.sql` before starting the
service. It creates only Identity Service-owned tables and uses idempotent
PostgreSQL setup. See [database/README.md](database/README.md) for execution,
ownership, and cross-service reference details. Hibernate remains configured
with `ddl-auto=validate` and does not generate runtime DDL.

## Security

Do not commit production credentials or JWT signing secrets. Use environment
variables for every deployment, and rotate secrets independently per
environment. The service never logs passwords, access tokens, or refresh
tokens.
