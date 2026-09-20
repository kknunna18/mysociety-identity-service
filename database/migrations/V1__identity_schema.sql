/*
 * Identity Service schema (PostgreSQL 16+).
 *
 * Extracted from mysociety_postgresql_complete.sql. This migration owns only
 * Identity Service tables. UUIDs that identify resources owned by other
 * services deliberately have no foreign keys here.
 */

BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE SCHEMA IF NOT EXISTS mysociety;
SET search_path TO mysociety, public;

CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;

CREATE OR REPLACE FUNCTION prevent_update_delete()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION '% records are append-only', TG_TABLE_NAME;
END;
$$;

CREATE TABLE IF NOT EXISTS app_users (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email               VARCHAR(254),
    mobile_number       VARCHAR(30),
    password_hash       VARCHAR(255),
    first_name          VARCHAR(100) NOT NULL,
    last_name           VARCHAR(100),
    profile_image_url   VARCHAR(500),
    preferred_language  VARCHAR(10) NOT NULL DEFAULT 'en',
    timezone            VARCHAR(60) NOT NULL DEFAULT 'Asia/Kolkata',
    email_verified      BOOLEAN NOT NULL DEFAULT FALSE,
    mobile_verified     BOOLEAN NOT NULL DEFAULT FALSE,
    mfa_enabled         BOOLEAN NOT NULL DEFAULT FALSE,
    failed_login_count  INTEGER NOT NULL DEFAULT 0 CHECK (failed_login_count >= 0),
    locked_until        TIMESTAMPTZ,
    last_login_at       TIMESTAMPTZ,
    status              VARCHAR(20) NOT NULL DEFAULT 'INVITED'
                        CHECK (status IN ('INVITED','ACTIVE','LOCKED','SUSPENDED','INACTIVE')),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version             BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_user_contact CHECK (email IS NOT NULL OR mobile_number IS NOT NULL)
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_app_users_email_lower
    ON app_users (LOWER(email)) WHERE email IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS ux_app_users_mobile
    ON app_users (mobile_number) WHERE mobile_number IS NOT NULL;

CREATE TABLE IF NOT EXISTS roles (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    society_id  UUID,
    code        VARCHAR(60) NOT NULL,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    is_system   BOOLEAN NOT NULL DEFAULT FALSE,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version     BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_roles_global_code
    ON roles (code) WHERE society_id IS NULL;
CREATE UNIQUE INDEX IF NOT EXISTS ux_roles_society_code
    ON roles (society_id, code) WHERE society_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS permissions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code        VARCHAR(100) NOT NULL UNIQUE,
    module_name VARCHAR(60) NOT NULL,
    description VARCHAR(500),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS role_permissions (
    role_id       UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE IF NOT EXISTS user_society_roles (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    society_id  UUID NOT NULL,
    user_id     UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    role_id     UUID NOT NULL REFERENCES roles(id) ON DELETE RESTRICT,
    valid_from  DATE NOT NULL DEFAULT CURRENT_DATE,
    valid_until DATE,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    granted_by  UUID REFERENCES app_users(id) ON DELETE SET NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version     BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_role_validity CHECK (valid_until IS NULL OR valid_until >= valid_from),
    UNIQUE (society_id, user_id, role_id, valid_from)
);

CREATE INDEX IF NOT EXISTS ix_user_society_roles_user
    ON user_society_roles (user_id, society_id, is_active);

CREATE TABLE IF NOT EXISTS refresh_tokens (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id              UUID NOT NULL REFERENCES app_users(id) ON DELETE CASCADE,
    token_hash           VARCHAR(255) NOT NULL UNIQUE,
    device_name          VARCHAR(150),
    ip_address           INET,
    user_agent           VARCHAR(500),
    issued_at            TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at           TIMESTAMPTZ NOT NULL,
    revoked_at           TIMESTAMPTZ,
    replaced_by_token_id UUID REFERENCES refresh_tokens(id) ON DELETE SET NULL,
    CONSTRAINT ck_refresh_expiry CHECK (expires_at > issued_at)
);

CREATE TABLE IF NOT EXISTS audit_events (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    society_id        UUID,
    actor_user_id     UUID REFERENCES app_users(id) ON DELETE SET NULL,
    actor_type        VARCHAR(20) NOT NULL DEFAULT 'USER'
                      CHECK (actor_type IN ('USER','SERVICE','SYSTEM','SUPPORT')),
    action            VARCHAR(100) NOT NULL,
    module_name       VARCHAR(60) NOT NULL,
    entity_type       VARCHAR(60),
    entity_id         UUID,
    outcome           VARCHAR(20) NOT NULL CHECK (outcome IN ('SUCCESS','FAILURE','DENIED')),
    correlation_id    VARCHAR(100),
    ip_address        INET,
    user_agent        VARCHAR(500),
    old_values        JSONB,
    new_values        JSONB,
    metadata          JSONB,
    occurred_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS ix_audit_tenant_time
    ON audit_events (society_id, occurred_at DESC);
CREATE INDEX IF NOT EXISTS ix_audit_entity
    ON audit_events (society_id, entity_type, entity_id, occurred_at DESC);
CREATE INDEX IF NOT EXISTS ix_audit_actor
    ON audit_events (actor_user_id, occurred_at DESC);

DROP TRIGGER IF EXISTS trg_audit_events_immutable ON audit_events;
CREATE TRIGGER trg_audit_events_immutable
BEFORE UPDATE OR DELETE ON audit_events
FOR EACH ROW EXECUTE FUNCTION prevent_update_delete();

CREATE TABLE IF NOT EXISTS outbox_events (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    society_id      UUID,
    aggregate_type  VARCHAR(80) NOT NULL,
    aggregate_id    UUID NOT NULL,
    event_type      VARCHAR(120) NOT NULL,
    event_version   INTEGER NOT NULL DEFAULT 1 CHECK (event_version > 0),
    payload         JSONB NOT NULL,
    headers         JSONB,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                    CHECK (status IN ('PENDING','PROCESSING','PUBLISHED','FAILED','DEAD')),
    attempt_count   INTEGER NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
    available_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at    TIMESTAMPTZ,
    last_error      VARCHAR(2000),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS ix_outbox_pending
    ON outbox_events (status, available_at, created_at)
    WHERE status IN ('PENDING','FAILED');

DO $$
DECLARE
    table_name TEXT;
BEGIN
    FOREACH table_name IN ARRAY ARRAY[
        'app_users','roles','user_society_roles'
    ]
    LOOP
        EXECUTE format(
            'DROP TRIGGER IF EXISTS %I ON %I',
            'trg_' || table_name || '_updated_at', table_name
        );
        EXECUTE format(
            'CREATE TRIGGER %I BEFORE UPDATE ON %I '
            'FOR EACH ROW EXECUTE FUNCTION set_updated_at()',
            'trg_' || table_name || '_updated_at', table_name
        );
    END LOOP;
END;
$$;

COMMIT;
