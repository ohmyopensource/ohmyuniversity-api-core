-- =============================================================
-- V4 - Fix calendar_event_type columns to varchar
--
-- Converts the calendar_event_type PostgreSQL enum columns
-- to varchar(20) to align with Hibernate's default enum mapping.
-- The application-level CalendarEventType enum enforces valid values.
-- =============================================================

ALTER TABLE calendar_event
    ALTER COLUMN type DROP DEFAULT;

ALTER TABLE university_event
    ALTER COLUMN type DROP DEFAULT;

ALTER TABLE calendar_event
ALTER COLUMN type TYPE varchar(20) USING type::varchar;

ALTER TABLE university_event
ALTER COLUMN type TYPE varchar(20) USING type::varchar;

ALTER TABLE calendar_event
    ALTER COLUMN type SET DEFAULT 'PERSONAL';

ALTER TABLE university_event
    ALTER COLUMN type SET DEFAULT 'UNIVERSITY';

DROP TYPE calendar_event_type;