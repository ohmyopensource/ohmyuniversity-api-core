-- =============================================================
-- V1 - Create omu_user table
--
-- Represents the stable OhMyUniversity identity of a person.
-- This is NOT a copy of Cineca data — it is the aggregating identity
-- that links one person across multiple universities and careers.
--
-- The codiceFiscale is the natural cross-university key: the same
-- person can have accounts at multiple universities but always has
-- the same Italian tax code.
--
-- No student data is stored here — all academic data is fetched
-- from Cineca on demand and cached in Redis.
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

COMMENT ON TABLE omu_user IS 'Stable OhMyUniversity identity. Aggregates one person across multiple universities and careers. No academic data stored — all fetched from Cineca on demand.';
COMMENT ON COLUMN omu_user.codice_fiscale IS 'Italian tax code — natural cross-university identifier. Used to match the same person across different university accounts.';
COMMENT ON COLUMN omu_user.email_primaria IS 'Primary contact email registered on OhMyUniversity. May differ from university institutional emails.';