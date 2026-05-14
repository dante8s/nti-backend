CREATE TABLE consultations (
                               id                UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
                               mentorship_id     UUID        NOT NULL REFERENCES mentorships(id) ON DELETE CASCADE,
                               mentor_id         BIGINT      NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
                               consultation_date DATE        NOT NULL,
                               topic             VARCHAR(255) NOT NULL,
                               description       TEXT,
                               created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                               updated_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_consultations_mentorship_id ON consultations(mentorship_id);
CREATE INDEX idx_consultations_mentor_id     ON consultations(mentor_id);