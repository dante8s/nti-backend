-- Milestone comments
CREATE TABLE milestone_comments (
                                    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
                                    milestone_id    UUID        NOT NULL REFERENCES milestones(id) ON DELETE CASCADE,
                                    content         TEXT        NOT NULL,
                                    created_by_id   BIGINT      NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
                                    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_milestone_comments_milestone_id ON milestone_comments(milestone_id);

-- Milestone attachments
CREATE TABLE milestone_attachments (
                                       id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
                                       milestone_id    UUID         NOT NULL REFERENCES milestones(id) ON DELETE CASCADE,
                                       file_name       VARCHAR(255) NOT NULL,
                                       file_path       VARCHAR(500) NOT NULL,
                                       uploaded_by_id  BIGINT       NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
                                       uploaded_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_milestone_attachments_milestone_id ON milestone_attachments(milestone_id);