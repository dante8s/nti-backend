CREATE TABLE mentorships (
                             id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
                             mentor_user_id  BIGINT      NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
                             application_id  BIGINT      NULL REFERENCES applications(id) ON DELETE SET NULL,
                             status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                             start_date      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                             end_date        TIMESTAMPTZ NULL,
                             created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE consultation_notes (
                                    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
                                    application_id  BIGINT      NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
                                    content         TEXT        NOT NULL,
                                    created_by_id   BIGINT      NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
                                    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
                                    updated_at      TIMESTAMPTZ NULL
);

CREATE INDEX idx_mentorships_mentor_user_id       ON mentorships(mentor_user_id);
CREATE INDEX idx_mentorships_status               ON mentorships(status);
CREATE INDEX idx_mentorships_application_id       ON mentorships(application_id);
CREATE INDEX idx_consultation_notes_application_id ON consultation_notes(application_id);
CREATE INDEX idx_consultation_notes_created_by_id  ON consultation_notes(created_by_id);