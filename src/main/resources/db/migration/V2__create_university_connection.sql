-- =============================================================
-- V2 - Create university_connection table
--
-- Links an OhMyUniversity user to a specific Cineca/ESSE3
-- university account.
--
-- Each user can have multiple connections:
-- - different universities
-- - multiple careers within the same university
--
-- IMPORTANT:
-- No Cineca credentials or tokens are stored here.
-- Session tokens are managed in Redis with TTL only.
-- =============================================================

CREATE TABLE university_connection
(
    id              UUID         NOT NULL DEFAULT gen_random_uuid(),
    user_id         UUID         NOT NULL,
    university_id   VARCHAR(20)  NOT NULL,
    university_name VARCHAR(255) NOT NULL,
    cineca_base_url VARCHAR(500) NOT NULL,
    username_cineca VARCHAR(255) NOT NULL,
    connected_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    last_used_at    TIMESTAMPTZ,

    CONSTRAINT pk_university_connection PRIMARY KEY (id),

    CONSTRAINT fk_university_connection_user
        FOREIGN KEY (user_id)
            REFERENCES omu_user (id)
            ON DELETE CASCADE,

    CONSTRAINT uq_university_connection
        UNIQUE (user_id, university_id, username_cineca)

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
ON TABLE university_connection IS
'Links an OhMyUniversity user to a Cineca/ESSE3 university account. A user may have multiple connections across universities or careers.';

COMMENT
ON COLUMN university_connection.id IS
'Primary key (UUID) generated via gen_random_uuid().';

COMMENT
ON COLUMN university_connection.user_id IS
'Reference to omu_user.id. Defines ownership of the connection.';

COMMENT
ON COLUMN university_connection.university_id IS
'Short university identifier (e.g. UNIMOL, UNIROMA1). Matches Cineca tenant identifier.';

COMMENT
ON COLUMN university_connection.university_name IS
'Human-readable name of the university.';

COMMENT
ON COLUMN university_connection.cineca_base_url IS
'Base URL of the ESSE3 REST API instance for the university.';

COMMENT
ON COLUMN university_connection.username_cineca IS
'Cineca ESSE3 username used for authentication at this university instance.';

COMMENT
ON COLUMN university_connection.connected_at IS
'Timestamp when the connection was created.';

COMMENT
ON COLUMN university_connection.last_used_at IS
'Timestamp of last usage of this connection.';