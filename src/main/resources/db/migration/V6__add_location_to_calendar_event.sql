-- =============================================================
-- V6 - Add location to calendar_event
--
-- Adds an optional location field to personal calendar events,
-- allowing students to specify a physical or virtual venue
-- (e.g. "Aula 5", "Google Meet", "Biblioteca centrale").
-- =============================================================

ALTER TABLE calendar_event
    ADD COLUMN location VARCHAR(500);

-- =============================================================
-- Comments
-- =============================================================

COMMENT ON COLUMN calendar_event.location IS
'Optional physical or virtual location of the event (e.g. Aula 5, meet.google.com/xyz). Null when not specified.';