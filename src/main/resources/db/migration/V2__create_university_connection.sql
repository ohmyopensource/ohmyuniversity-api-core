-- =============================================================
-- V2 - Create university_connection table
--
-- Maps to: UniversityConnection.java
-- Represents a link between an OhMyUniversity user and a specific
-- university account (Cineca/ESSE3 instance).
--
-- One user can have multiple connections (different universities,
-- or different careers at the same university with different usernames).
--
-- No Cineca tokens are stored here — they live in Redis with TTL only.
-- =============================================================

CREATE TABLE university_connection (
                                       id                  UUID        NOT NULL DEFAULT gen_random_uuid(),
                                       user_id             UUID        NOT NULL,
                                       university_id       VARCHAR(20) NOT NULL,
                                       university_name     VARCHAR(255) NOT NULL,
                                       cineca_base_url     VARCHAR(500) NOT NULL,
                                       username_cineca     VARCHAR(255) NOT NULL,
                                       connected_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
                                       last_used_at        TIMESTAMPTZ,

                                       CONSTRAINT pk_university_connection PRIMARY KEY (id),
                                       CONSTRAINT fk_university_connection_user
                                           FOREIGN KEY (user_id) REFERENCES omu_user (id) ON DELETE CASCADE,
                                       CONSTRAINT uq_university_connection
                                           UNIQUE (user_id, university_id, username_cineca)
);

CREATE INDEX idx_university_connection_user_id
    ON university_connection (user_id);

COMMENT ON TABLE university_connection IS 'Links an OhMyUniversity user to a specific Cineca/ESSE3 university account. One user can have multiple connections (different universities or different usernames).';
COMMENT ON COLUMN university_connection.university_id IS 'Short university identifier, e.g. "UNIMOL", "UNIROMA1". Matches the tenant field in the Cineca JWT.';
COMMENT ON COLUMN university_connection.cineca_base_url IS 'Base URL of the ESSE3 REST API for this university, e.g. "https://unimol.esse3.cineca.it/e3rest/api".';
COMMENT ON COLUMN university_connection.username_cineca IS 'Username used to authenticate against this university ESSE3 instance, e.g. "a.delmuto".';