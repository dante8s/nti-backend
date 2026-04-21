
DROP INDEX idx_consultation_notes_mentorship_id;

ALTER TABLE consultation_notes
DROP COLUMN mentorship_id,
    ADD COLUMN application_id BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN updated_at TIMESTAMPTZ NULL;

ALTER TABLE consultation_notes
    ALTER COLUMN application_id DROP DEFAULT;

CREATE INDEX idx_consultation_notes_application_id ON consultation_notes(application_id);