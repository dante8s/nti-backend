-- Snapshot of team members at the time of application submission
CREATE TABLE IF NOT EXISTS application_members (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL,
    email VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_application_members_application_id ON application_members(application_id);
CREATE INDEX IF NOT EXISTS idx_application_members_user_id ON application_members(user_id);

-- New COMPLETED status for applications (if a CHECK constraint is used)
-- PostgreSQL enum or string values — nothing to add,
-- because the status is stored as VARCHAR via @Enumerated(EnumType.STRING)
