-- =============================================================
-- V5 - Create cineca_sync_state table
--
-- Tracks which Kafka integration events have already been
-- published for a given user/university/course combination.
--
-- This table acts as a deduplication guard for the Cineca sync
-- process: each time a student logs in, OhMyU compares the
-- current Cineca libretto against this table to determine
-- which events (course-edition.discovered, enrollment.discovered,
-- teaching-assignment.discovered, campus-assignment.discovered)
-- are new and need to be published.
--
-- IMPORTANT:
-- This table does NOT store academic or career data.
-- It only tracks notification state for Kafka event publishing.
-- A row means "we already told the downstream services about
-- this fact" — nothing more.
--
-- Sync model:
-- Events are triggered per-user at login time, using the
-- student's own Cineca JWT (scoped to that user only).
-- A global batch sync across all students requires a
-- UTENTE_TECNICO Cineca account (not yet available).
-- =============================================================

CREATE TABLE cineca_sync_state
(
    id                  UUID         NOT NULL DEFAULT gen_random_uuid(),
    user_id             UUID         NOT NULL,
    university_id       VARCHAR(20)  NOT NULL,
    adsce_id            BIGINT       NOT NULL,
    event_type          VARCHAR(50)  NOT NULL,
    external_channel_id VARCHAR(255),
    notified_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT pk_cineca_sync_state PRIMARY KEY (id),

    CONSTRAINT fk_cineca_sync_state_user
        FOREIGN KEY (user_id)
            REFERENCES omu_user (id)
            ON DELETE CASCADE,

    CONSTRAINT uq_cineca_sync_state
        UNIQUE (user_id, university_id, adsce_id, event_type)
);

-- =============================================================
-- Indexes
-- =============================================================

CREATE INDEX idx_cineca_sync_state_user_university
    ON cineca_sync_state (user_id, university_id);

CREATE INDEX idx_cineca_sync_state_adsce
    ON cineca_sync_state (adsce_id);

-- =============================================================
-- Comments
-- =============================================================

COMMENT ON TABLE cineca_sync_state IS
'Deduplication guard for Kafka event publishing. Each row represents a fact already notified to downstream services (api-chat, api-canteen) via a Kafka integration event.';

COMMENT ON COLUMN cineca_sync_state.id IS
'Primary key (UUID) generated via gen_random_uuid().';

COMMENT ON COLUMN cineca_sync_state.user_id IS
'Reference to omu_user.id. Identifies the student whose libretto triggered the event.';

COMMENT ON COLUMN cineca_sync_state.university_id IS
'Short university identifier (e.g. UNIMOL). Scopes the sync state to a specific Cineca tenant.';

COMMENT ON COLUMN cineca_sync_state.adsce_id IS
'Cineca activity identifier (adsceId) from the libretto row. Identifies the specific course edition within the sync context.';

COMMENT ON COLUMN cineca_sync_state.event_type IS
'Type of Kafka event already published. One of: COURSE_EDITION, ENROLLMENT, TEACHING_ASSIGNMENT, CAMPUS_ASSIGNMENT.';

COMMENT ON COLUMN cineca_sync_state.external_channel_id IS
'Deterministic channel identifier sent in the Kafka event payload (format: {course-slug}-{university-slug}-{year}-{semester}). Null for CAMPUS_ASSIGNMENT events.';

COMMENT ON COLUMN cineca_sync_state.notified_at IS
'Timestamp when the Kafka event was published.';