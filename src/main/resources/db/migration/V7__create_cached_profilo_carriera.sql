-- =============================================================
-- V7 - Create cached_profilo_carriera table
--
-- Persists the academic career profiles returned by Cineca ESSE3
-- at login time, scoped per user and university.
--
-- This table enables multi-university profile aggregation:
-- when a user logs in with any university account, all their
-- previously cached profiles (from other universities) are
-- returned in the LoginResponse alongside the current ones.
-- This avoids requiring the user to re-authenticate with every
-- university to see their aggregated profile list.
--
-- IMPORTANT:
-- This table stores a snapshot of Cineca career data as of the
-- last login for each university. It is NOT a source of truth
-- for academic data — it is a read cache for UI purposes only.
-- Data is upserted at every login and may become stale if the
-- student's career status changes between logins.
--
-- Profile aggregation model:
-- Profiles are grouped by omu_user_id (which is unique per
-- codice fiscale). At login, profiles for the current university
-- are upserted, and all profiles across all universities are
-- returned. This allows the avatar panel to show all known
-- career profiles regardless of which university the user
-- logged in with.
-- =============================================================

CREATE TABLE cached_profilo_carriera
(
    id               UUID         NOT NULL DEFAULT gen_random_uuid(),
    omu_user_id      UUID         NOT NULL,
    university_id    VARCHAR(20)  NOT NULL,
    university_name  VARCHAR(255) NOT NULL,
    stu_id           BIGINT       NOT NULL,
    mat_id           BIGINT       NOT NULL,
    matricola        VARCHAR(50),
    corso_nome       VARCHAR(255),
    corso_codice     VARCHAR(50),
    cds_id           BIGINT,
    tipo_corso_cod   VARCHAR(20),
    status_studente  VARCHAR(10),
    status_descr     VARCHAR(100),
    anno_corso       INTEGER,
    durata_anni      INTEGER,
    anno_accademico  INTEGER,
    attivo           BOOLEAN      NOT NULL DEFAULT FALSE,
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_cached_profilo_carriera
        PRIMARY KEY (id),

    CONSTRAINT fk_cached_profilo_carriera_user
        FOREIGN KEY (omu_user_id)
            REFERENCES omu_user (id)
            ON DELETE CASCADE,

    CONSTRAINT uq_cached_profilo_carriera_user_stu
        UNIQUE (omu_user_id, stu_id)
);

-- =============================================================
-- Indexes
-- =============================================================

CREATE INDEX idx_cached_profilo_carriera_user
    ON cached_profilo_carriera (omu_user_id);

CREATE INDEX idx_cached_profilo_carriera_university
    ON cached_profilo_carriera (omu_user_id, university_id);

-- =============================================================
-- Comments
-- =============================================================

COMMENT ON TABLE cached_profilo_carriera IS
'Read cache for Cineca career profiles. Populated at login time and used to aggregate multi-university profiles in the LoginResponse without requiring re-authentication.';

COMMENT ON COLUMN cached_profilo_carriera.id IS
'Primary key (UUID) generated via gen_random_uuid().';

COMMENT ON COLUMN cached_profilo_carriera.omu_user_id IS
'Reference to omu_user.id. Groups all profiles for the same physical person across universities.';

COMMENT ON COLUMN cached_profilo_carriera.university_id IS
'Short university identifier (e.g. UNIMOL, UNIPI). Scopes the profile to a specific Cineca tenant.';

COMMENT ON COLUMN cached_profilo_carriera.university_name IS
'Human-readable university name as returned by Cineca at login time.';

COMMENT ON COLUMN cached_profilo_carriera.stu_id IS
'Cineca student identifier (stuId). Unique per career track across universities.';

COMMENT ON COLUMN cached_profilo_carriera.mat_id IS
'Cineca matricola identifier (matId). Used for career-specific API calls.';

COMMENT ON COLUMN cached_profilo_carriera.matricola IS
'Student registration number (matricola). Displayed in the UI profile panel.';

COMMENT ON COLUMN cached_profilo_carriera.corso_nome IS
'Degree course name as returned by Cineca (cdsDes).';

COMMENT ON COLUMN cached_profilo_carriera.corso_codice IS
'Degree course code as returned by Cineca (cdsCod).';

COMMENT ON COLUMN cached_profilo_carriera.cds_id IS
'Cineca degree course identifier (cdsId). Required for exam session endpoints.';

COMMENT ON COLUMN cached_profilo_carriera.tipo_corso_cod IS
'Cineca course type code (tipoCorsoCod). Used to derive the course acronym (L, LM, etc.).';

COMMENT ON COLUMN cached_profilo_carriera.status_studente IS
'Cineca student status code (staStuCod). Values: A=active, X=withdrawn, etc.';

COMMENT ON COLUMN cached_profilo_carriera.status_descr IS
'Human-readable description of the student status (staStuDes).';

COMMENT ON COLUMN cached_profilo_carriera.anno_corso IS
'Current year of the degree course (annoCorso).';

COMMENT ON COLUMN cached_profilo_carriera.durata_anni IS
'Total duration of the degree course in years (durataAnni).';

COMMENT ON COLUMN cached_profilo_carriera.anno_accademico IS
'Academic year of enrollment (aaIscrId).';

COMMENT ON COLUMN cached_profilo_carriera.attivo IS
'Whether this career profile is currently active (staStuCod = A).';

COMMENT ON COLUMN cached_profilo_carriera.updated_at IS
'Timestamp of the last upsert. Updated at every login for the corresponding university.';