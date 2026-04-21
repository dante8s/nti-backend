CREATE TABLE programs (
                          id          BIGSERIAL    PRIMARY KEY,
                          name        VARCHAR(255) NOT NULL,
                          description TEXT,
                          type        VARCHAR(20)  NOT NULL,
                          is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
                          created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_programs_type
    ON programs(type);
CREATE INDEX idx_programs_active
    ON programs(is_active);