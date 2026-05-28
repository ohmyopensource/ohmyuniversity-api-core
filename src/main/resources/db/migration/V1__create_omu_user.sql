-- =============================================================
-- V1 - Create omu_user table
--
-- Core identity table for OhMyUniversity.
-- Represents a stable, cross-university user identity.
--
-- IMPORTANT:
-- This table does NOT store academic data.
-- All student data is retrieved from Cineca APIs on demand
-- and cached in Redis.
-- =============================================================

CREATE TABLE omu_user (
                          id                  UUID        NOT NULL DEFAULT gen_random_uuid(),
                          codice_fiscale      VARCHAR(16) NOT NULL,
                          email_primaria      VARCHAR(255),
                          created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
                          last_login_at       TIMESTAMPTZ,

                          CONSTRAINT pk_omu_user PRIMARY KEY (id),
                          CONSTRAINT uq_omu_user_codice_fiscale UNIQUE (codice_fiscale)
);

-- =============================================================
-- Comments
-- =============================================================

COMMENT ON TABLE omu_user IS
'Core identity table for OhMyUniversity. Represents a user across multiple universities. No academic data is stored here.';

COMMENT ON COLUMN omu_user.id IS
'Internal UUID primary key generated via gen_random_uuid().';

COMMENT ON COLUMN omu_user.codice_fiscale IS
'Italian tax code used as cross-university unique identifier for a person.';

COMMENT ON COLUMN omu_user.email_primaria IS
'Primary email used for OhMyUniversity communications (may differ from university email).';

COMMENT ON COLUMN omu_user.created_at IS
'Timestamp of user creation.';

COMMENT ON COLUMN omu_user.last_login_at IS
'Timestamp of last successful authentication.';