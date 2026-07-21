-- =============================================================
-- V4 - Create cached_profilo_carriera table
--
-- Persists the academic career profiles returned by the platform
-- vendor at login time, scoped per user and university.
--
-- This table enables multi-university profile aggregation: the
-- core service listens to the auth service's user.authenticated
-- Kafka event and upserts a row per career here, so the avatar
-- panel can show all known career profiles for a person regardless
-- of which university they most recently logged in with.
--
-- IMPORTANT:
-- This table stores a snapshot of vendor career data as of the
-- last login for each university. It is NOT a source of truth
-- for academic data - it is a read cache for UI purposes only.
-- Data is upserted at every login and may become stale if the
-- student's career status changes between logins.
-- =============================================================

CREATE TABLE cached_profilo_carriera
(
    id              UUID         NOT NULL DEFAULT gen_random_uuid(),
    omu_user_id     UUID         NOT NULL,
    university_id   VARCHAR(20)  NOT NULL,
    university_name VARCHAR(255) NOT NULL,
    stu_id          BIGINT       NOT NULL,
    mat_id          BIGINT       NOT NULL,
    matricola       VARCHAR(50),
    corso_nome      VARCHAR(255),
    corso_codice    VARCHAR(50),
    cds_id          BIGINT,
    tipo_corso_cod  VARCHAR(20),
    status_studente VARCHAR(10),
    status_descr    VARCHAR(100),
    anno_corso      INTEGER,
    durata_anni     INTEGER,
    anno_accademico INTEGER,
    attivo          BOOLEAN      NOT NULL DEFAULT FALSE,
    laureato        BOOLEAN      NOT NULL DEFAULT FALSE,
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),

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

COMMENT
ON TABLE cached_profilo_carriera IS
'Read cache for vendor career profiles. Populated asynchronously via the user.authenticated Kafka event, used to aggregate multi-university profiles for the avatar panel without requiring re-authentication.';

COMMENT
ON COLUMN cached_profilo_carriera.id IS
'Primary key (UUID) generated via gen_random_uuid().';

COMMENT
ON COLUMN cached_profilo_carriera.omu_user_id IS
'Reference to omu_user.id. Groups all profiles for the same physical person across universities.';

COMMENT
ON COLUMN cached_profilo_carriera.university_id IS
'Short university identifier (e.g. UNIMOL, UNIPI). Scopes the profile to a specific vendor tenant.';

COMMENT
ON COLUMN cached_profilo_carriera.university_name IS
'Human-readable university name as returned by the vendor at login time.';

COMMENT
ON COLUMN cached_profilo_carriera.stu_id IS
'Vendor student identifier (Cineca stuId). Unique per career track across universities.';

COMMENT
ON COLUMN cached_profilo_carriera.mat_id IS
'Vendor matricola identifier (Cineca matId). Used for career-specific API calls.';

COMMENT
ON COLUMN cached_profilo_carriera.matricola IS
'Student registration number (matricola). Displayed in the UI profile panel.';

COMMENT
ON COLUMN cached_profilo_carriera.corso_nome IS
'Degree course name as returned by the vendor (Cineca cdsDes).';

COMMENT
ON COLUMN cached_profilo_carriera.corso_codice IS
'Degree course code as returned by the vendor (Cineca cdsCod).';

COMMENT
ON COLUMN cached_profilo_carriera.cds_id IS
'Vendor degree course identifier (Cineca cdsId). Required for exam session endpoints.';

COMMENT
ON COLUMN cached_profilo_carriera.tipo_corso_cod IS
'Vendor course type code (Cineca tipoCorsoCod). Used to derive the course acronym (L, LM, etc.).';

COMMENT
ON COLUMN cached_profilo_carriera.status_studente IS
'Vendor student status code (Cineca staStuCod). Values: A=active, X=withdrawn, etc.';

COMMENT
ON COLUMN cached_profilo_carriera.status_descr IS
'Human-readable description of the student status (Cineca staStuDes).';

COMMENT
ON COLUMN cached_profilo_carriera.anno_corso IS
'Current year of the degree course (Cineca annoCorso).';

COMMENT
ON COLUMN cached_profilo_carriera.durata_anni IS
'Total duration of the degree course in years (Cineca durataAnni).';

COMMENT
ON COLUMN cached_profilo_carriera.anno_accademico IS
'Academic year of enrollment (Cineca aaIscrId).';

COMMENT
ON COLUMN cached_profilo_carriera.attivo IS
'Whether this career profile is currently active (staStuCod = A).';

COMMENT
ON COLUMN cached_profilo_carriera.laureato IS
'Whether the student obtained a degree from this career track. Derived from Cineca attlauFlg (1 = graduated, 0 = otherwise ceased). Used to differentiate ring color in the avatar panel.';

COMMENT
ON COLUMN cached_profilo_carriera.updated_at IS
'Timestamp of the last upsert, refreshed at every login for the corresponding university.';