-- =============================================================
-- V1 - Create identity tables
--
-- Tables:
--   omu_user             - core cross-university identity
--   university_connection - links a user to a platform vendor account
--
-- Design notes:
--   omu_user does NOT store academic data. All career data is
--   retrieved from vendor APIs on demand and cached separately
--   (see cached_profilo_carriera).
--
--   university_connection does NOT store vendor credentials or
--   session tokens - those are managed in Redis with TTL only,
--   by the auth service's AuthSessionStore.
--
--   Column naming is vendor-agnostic (vendor_base_url,
--   username_vendor) rather than Cineca-specific, since a
--   university may in principle run a different platform vendor
--   (GOMP, Multiversity, ...), not just Cineca ESSE3.
-- =============================================================

CREATE TABLE omu_user
(
    id             UUID        NOT NULL DEFAULT gen_random_uuid(),
    codice_fiscale VARCHAR(16) NOT NULL,
    email_primaria VARCHAR(255),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_login_at  TIMESTAMPTZ,

    CONSTRAINT pk_omu_user PRIMARY KEY (id),
    CONSTRAINT uq_omu_user_codice_fiscale UNIQUE (codice_fiscale)
);

CREATE TABLE university_connection
(
    id              UUID         NOT NULL DEFAULT gen_random_uuid(),
    user_id         UUID         NOT NULL,
    university_id   VARCHAR(20)  NOT NULL,
    university_name VARCHAR(255) NOT NULL,
    vendor_base_url VARCHAR(500) NOT NULL,
    username_vendor VARCHAR(255) NOT NULL,
    connected_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    last_used_at    TIMESTAMPTZ,

    CONSTRAINT pk_university_connection PRIMARY KEY (id),

    CONSTRAINT fk_university_connection_user
        FOREIGN KEY (user_id)
            REFERENCES omu_user (id)
            ON DELETE CASCADE,

    CONSTRAINT uq_university_connection
        UNIQUE (user_id, university_id, username_vendor)
);

-- =============================================================
-- Indexes
-- =============================================================

CREATE INDEX idx_university_connection_user_id
    ON university_connection (user_id);

-- =============================================================
-- Comments
-- =============================================================

COMMENT
ON TABLE omu_user IS
'Core identity table for OhMyUniversity. Represents a user across multiple universities and platform vendors. No academic data is stored here.';

COMMENT
ON COLUMN omu_user.id IS
'Internal UUID primary key generated via gen_random_uuid().';

COMMENT
ON COLUMN omu_user.codice_fiscale IS
'Italian tax code used as cross-university unique identifier for a person.';

COMMENT
ON COLUMN omu_user.email_primaria IS
'Primary email used for OhMyUniversity communications (may differ from university email).';

COMMENT
ON COLUMN omu_user.created_at IS
'Timestamp of user creation.';

COMMENT
ON COLUMN omu_user.last_login_at IS
'Timestamp of last successful authentication, across any vendor/university.';

COMMENT
ON TABLE university_connection IS
'Links an OhMyUniversity user to a platform vendor account (Cineca/ESSE3, GOMP, Multiversity, ...). A user may have multiple connections across universities, careers, or vendors.';

COMMENT
ON COLUMN university_connection.id IS
'Primary key (UUID) generated via gen_random_uuid().';

COMMENT
ON COLUMN university_connection.user_id IS
'Reference to omu_user.id. Defines ownership of the connection.';

COMMENT
ON COLUMN university_connection.university_id IS
'Short university identifier (e.g. UNIMOL, UNIROMA1). Matches UniversityRegistry keys.';

COMMENT
ON COLUMN university_connection.university_name IS
'Human-readable name of the university.';

COMMENT
ON COLUMN university_connection.vendor_base_url IS
'Base URL of the platform vendor API instance for this university (e.g. Cineca ESSE3 REST API base URL).';

COMMENT
ON COLUMN university_connection.username_vendor IS
'Username used to authenticate against the vendor system at this university instance.';

COMMENT
ON COLUMN university_connection.connected_at IS
'Timestamp when the connection was created.';

COMMENT
ON COLUMN university_connection.last_used_at IS
'Timestamp of last usage of this connection.';