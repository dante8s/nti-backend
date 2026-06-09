CREATE TABLE project_reports (
    id                  BIGSERIAL PRIMARY KEY,
    application_id      BIGINT NOT NULL REFERENCES applications(id),
    project_name        VARCHAR(500),
    program_type        VARCHAR(50) NOT NULL,
    team_leader_name    VARCHAR(255),
    team_members        TEXT,
    product_owner_name  VARCHAR(255),
    completed_at        TIMESTAMP NOT NULL,
    kpi_score           DOUBLE PRECISION,
    kpi_details         TEXT,
    result_documents    TEXT,
    milestones_total    INT NOT NULL DEFAULT 0,
    milestones_done     INT NOT NULL DEFAULT 0,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW()
);
