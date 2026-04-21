CREATE TABLE mentorships (
                             id              UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
                             mentor_user_id  BIGINT  NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
                             application_id  BIGINT  NULL,       -- placeholder for Application entity
                             status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
                             start_date      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                             end_date        TIMESTAMPTZ  NULL,
                             created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE consultation_notes (
                                    id              UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
                                    mentorship_id   UUID    NOT NULL REFERENCES mentorships(id) ON DELETE CASCADE,
                                    content         TEXT    NOT NULL,
                                    created_by_id   BIGINT  NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
                                    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_mentorships_mentor_user_id  ON mentorships(mentor_user_id);
CREATE INDEX idx_mentorships_status          ON mentorships(status);
CREATE INDEX idx_consultation_notes_mentorship_id ON consultation_notes(mentorship_id);