-- =============================================================
-- V8 - Add laureato column to cached_profilo_carriera
--
-- Adds the `laureato` flag to the career profile cache table,
-- derived from Cineca's `attlauFlg` field returned at login.
--
-- This flag distinguishes between two types of ceased careers:
-- - laureato = true  => career ended with degree conferral
--                      (attlauFlg = 1 in Cineca response)
-- - laureato = false => career ended for other reasons
--                      (withdrawal, transfer, administrative)
--
-- This distinction is used in the avatar panel to render the
-- correct ring color for non-active profiles:
-- - graduated (blue ring) => laureato = true
-- - withdrawn (grey ring) => laureato = false
--
-- The default value is FALSE to preserve correct behavior for
-- all existing cached profiles, which will be updated on the
-- next login by the upsert logic in AuthService.
-- =============================================================

ALTER TABLE cached_profilo_carriera
    ADD COLUMN laureato BOOLEAN NOT NULL DEFAULT FALSE;

-- =============================================================
-- Comments
-- =============================================================

COMMENT ON COLUMN cached_profilo_carriera.laureato IS
'Whether the student obtained a degree from this career track. Derived from Cineca attlauFlg (1 = graduated, 0 = otherwise ceased). Used to differentiate ring color in the avatar panel.';