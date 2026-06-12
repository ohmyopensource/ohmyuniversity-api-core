-- =============================================================
-- V3 - Create calendar tables

-- Tables:
--   calendar_event_type    — PostgreSQL enum for event classification
--   calendar_event         — personal events owned by an OhMyU student
--   university_event       — events published by a university admin
--   calendar_event_import  — bridge: tracks imports of university events
--
-- Design notes:
--   The calendar is cross-university: events belong to the OhMyU
--   user identity, not to a specific university enrollment.
--   University events are shared across all students of a university;
--   the import bridge records which students added them personally.
-- =============================================================

CREATE TYPE calendar_event_type AS ENUM (
    'PERSONAL',
    'EXAM',
    'DEADLINE',
    'REMINDER',
    'UNIVERSITY'
);

-- =============================================================
-- calendar_event
-- =============================================================

CREATE TABLE calendar_event
(
    id          UUID                     NOT NULL DEFAULT gen_random_uuid(),
    omu_user_id UUID                     NOT NULL,
    title       VARCHAR(255)             NOT NULL,
    description TEXT,
    start_date  TIMESTAMPTZ              NOT NULL,
    end_date    TIMESTAMPTZ,
    all_day     BOOLEAN                  NOT NULL DEFAULT FALSE,
    type        calendar_event_type      NOT NULL DEFAULT 'PERSONAL',
    color       VARCHAR(7),
    url         VARCHAR(2048),
    notes       TEXT,
    created_at  TIMESTAMPTZ              NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ              NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_calendar_event PRIMARY KEY (id),
    CONSTRAINT fk_calendar_event_user FOREIGN KEY (omu_user_id)
        REFERENCES omu_user (id) ON DELETE CASCADE
);

-- =============================================================
-- Indexes
-- =============================================================

CREATE INDEX idx_calendar_event_user  ON calendar_event (omu_user_id);
CREATE INDEX idx_calendar_event_start ON calendar_event (start_date);

-- =============================================================
-- Comments
-- =============================================================

COMMENT ON TABLE calendar_event IS
'Personal calendar events owned by an OhMyU student. Shared across all university enrollments.';

COMMENT ON COLUMN calendar_event.id IS
'Internal UUID primary key generated via gen_random_uuid().';

COMMENT ON COLUMN calendar_event.omu_user_id IS
'Reference to the owning OhMyU user. Cascade delete removes events when the user is deleted.';

COMMENT ON COLUMN calendar_event.title IS
'Short display title shown in the calendar widget.';

COMMENT ON COLUMN calendar_event.description IS
'Optional longer description of the event.';

COMMENT ON COLUMN calendar_event.start_date IS
'Event start timestamp (UTC). If all_day is true, the time component is ignored by the client.';

COMMENT ON COLUMN calendar_event.end_date IS
'Optional event end timestamp (UTC). Null means point-in-time event.';

COMMENT ON COLUMN calendar_event.all_day IS
'If true, the event spans the full day and time components are ignored by the client.';

COMMENT ON COLUMN calendar_event.type IS
'Event classification using the calendar_event_type enum. Drives frontend icon and color logic.';

COMMENT ON COLUMN calendar_event.color IS
'Optional hex color code (e.g. #FF5733) overriding the default type color in the frontend widget.';

COMMENT ON COLUMN calendar_event.url IS
'Optional external URL relevant to the event (e.g. exam booking page, meeting link).';

COMMENT ON COLUMN calendar_event.notes IS
'Free-form multiline notes attached to the event.';

COMMENT ON COLUMN calendar_event.created_at IS
'Timestamp of event creation (set once at insert).';

COMMENT ON COLUMN calendar_event.updated_at IS
'Timestamp of last update. Must be refreshed on every write.';

-- =============================================================
-- university_event
-- =============================================================

CREATE TABLE university_event
(
    id            UUID                     NOT NULL DEFAULT gen_random_uuid(),
    university_id VARCHAR(50)              NOT NULL,
    title         VARCHAR(255)             NOT NULL,
    description   TEXT,
    start_date    TIMESTAMPTZ              NOT NULL,
    end_date      TIMESTAMPTZ,
    all_day       BOOLEAN                  NOT NULL DEFAULT FALSE,
    type          calendar_event_type      NOT NULL DEFAULT 'UNIVERSITY',
    color         VARCHAR(7),
    url           VARCHAR(2048),
    source_url    VARCHAR(2048),
    published_at  TIMESTAMPTZ              NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_university_event PRIMARY KEY (id)
);

-- =============================================================
-- Indexes
-- =============================================================

CREATE INDEX idx_university_event_uni ON university_event (university_id);
CREATE INDEX idx_university_event_start ON university_event (start_date);

-- =============================================================
-- Comments
-- =============================================================

COMMENT ON TABLE university_event IS
'Events published by a university and visible to all its enrolled students.';

COMMENT ON COLUMN university_event.id IS
'Internal UUID primary key generated via gen_random_uuid().';

COMMENT ON COLUMN university_event.university_id IS
'Identifier of the publishing university (e.g. UNIMOL). Matches UniversityRegistry keys.';

COMMENT ON COLUMN university_event.title IS
'Short display title of the university event.';

COMMENT ON COLUMN university_event.description IS
'Optional longer description provided by the university.';

COMMENT ON COLUMN university_event.start_date IS
'Event start timestamp (UTC).';

COMMENT ON COLUMN university_event.end_date IS
'Optional event end timestamp (UTC).';

COMMENT ON COLUMN university_event.all_day IS
'If true, the event spans the full day and time components are ignored by the client.';

COMMENT ON COLUMN university_event.type IS
'Event classification. Typically UNIVERSITY for all records in this table.';

COMMENT ON COLUMN university_event.color IS
'Optional hex color code for frontend rendering.';

COMMENT ON COLUMN university_event.url IS
'Optional link to an external page related to the event.';

COMMENT ON COLUMN university_event.source_url IS
'URL of the original source from which this event was scraped or imported.';

COMMENT ON COLUMN university_event.published_at IS
'Timestamp when the event was published into the system.';

-- =============================================================
-- calendar_event_import
-- =============================================================

CREATE TABLE calendar_event_import
(
    id                  UUID        NOT NULL DEFAULT gen_random_uuid(),
    omu_user_id         UUID        NOT NULL,
    university_event_id UUID        NOT NULL,
    imported_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_calendar_event_import PRIMARY KEY (id),
    CONSTRAINT uq_calendar_import UNIQUE (omu_user_id, university_event_id),
    CONSTRAINT fk_calendar_import_user FOREIGN KEY (omu_user_id)
        REFERENCES omu_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_calendar_import_event FOREIGN KEY (university_event_id)
        REFERENCES university_event (id) ON DELETE CASCADE
);

-- =============================================================
-- Indexes
-- =============================================================

CREATE INDEX idx_calendar_import_user ON calendar_event_import (omu_user_id);

-- =============================================================
-- Comments
-- =============================================================

COMMENT ON TABLE calendar_event_import IS
'Bridge table tracking which university events each student has imported into their calendar.';

COMMENT ON COLUMN calendar_event_import.id IS
'Internal UUID primary key generated via gen_random_uuid().';

COMMENT ON COLUMN calendar_event_import.omu_user_id IS
'Reference to the student who imported the event. Cascade delete on user removal.';

COMMENT ON COLUMN calendar_event_import.university_event_id IS
'Reference to the imported university event. Cascade delete if the event is removed.';

COMMENT ON COLUMN calendar_event_import.imported_at IS
'Timestamp when the student imported the university event into their personal calendar.';