CREATE TABLE program_b_requirements (
    id                  BIGSERIAL    PRIMARY KEY,
    program_id      BIGINT       NOT NULL REFERENCES programs(id) ON DELETE CASCADE,
    specification_name  VARCHAR(255) NULL,
    specification_path  VARCHAR(500) NULL,
    budget_name         VARCHAR(255) NULL,
    budget_path         VARCHAR(500) NULL,
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_program_b_req_program_id ON program_b_requirements(program_id);