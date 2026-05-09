CREATE TABLE milestones (
                            id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
                            application_id  BIGINT       NULL REFERENCES applications(id) ON DELETE CASCADE,
                            mentorship_id   UUID         NULL REFERENCES mentorships(id) ON DELETE SET NULL,
                            title           VARCHAR(255) NOT NULL,
                            description     TEXT,
                            due_date        DATE         NOT NULL,
                            completed_at    TIMESTAMPTZ  NULL,
                            status          VARCHAR(20)  NOT NULL DEFAULT 'PLANNED',
                            created_by_id   BIGINT       NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
                            created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                            updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_milestones_application_id  ON milestones(application_id);
CREATE INDEX idx_milestones_mentorship_id   ON milestones(mentorship_id);
CREATE INDEX idx_milestones_created_by_id   ON milestones(created_by_id);
CREATE INDEX idx_milestones_status          ON milestones(status);
CREATE INDEX idx_milestones_due_date        ON milestones(due_date);