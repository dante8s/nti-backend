CREATE TABLE programs (
                          id              BIGSERIAL    PRIMARY KEY,
                          name            VARCHAR(255) NOT NULL,
                          description     TEXT,
                          type            VARCHAR(20)  NOT NULL,
                          organization_id UUID         NULL REFERENCES organizations(id) ON DELETE SET NULL,
                          status          VARCHAR(20)  NOT NULL DEFAULT 'APPROVED',
                          admin_comment   TEXT         NULL,
                          created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
                          updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_programs_type             ON programs(type);
CREATE INDEX idx_programs_status           ON programs(status);
CREATE INDEX idx_programs_organization_id  ON programs(organization_id);