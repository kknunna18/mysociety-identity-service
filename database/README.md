# Identity Service database migrations

`migrations/V1__identity_schema.sql` is the versioned bootstrap schema for the
Identity Service. Apply migrations in lexical version order, once per database
environment, from a deployment job or DBA-controlled process. The script is
also safe to rerun for its schema, extension, indexes, functions, and
triggers; it does not alter or remove existing application data.

## Execution

The target is PostgreSQL 16 or later. A database administrator must install
`pgcrypto` (or grant the deploying role permission to create that extension)
and grant the Identity Service owner permission to create objects in the
`mysociety` schema.

```powershell
psql -v ON_ERROR_STOP=1 -U identity_service -d mysociety `
  -f database/migrations/V1__identity_schema.sql
```

The service continues to use Hibernate `ddl-auto=validate`; it does not
execute migrations or generate DDL at runtime. Production deployment should
record the applied version in the deployment system's migration history before
starting the application.

## Ownership and service boundaries

This migration owns `app_users`, `roles`, `permissions`, `role_permissions`,
`user_society_roles`, `refresh_tokens`, `audit_events`, and `outbox_events`,
along with the two supporting trigger functions. `audit_events` and
`outbox_events` are included because they are mapped by the Identity Service.

`society_id` is an external reference owned by the Society service. It remains
a UUID and participates in canonical indexes and uniqueness rules, but has no
foreign key in this migration. Its existence is validated through Society
Service APIs and domain events. All foreign keys between Identity-owned tables
are retained.
