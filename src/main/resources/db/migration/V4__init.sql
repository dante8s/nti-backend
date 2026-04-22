CREATE TABLE calls (
                       id                  BIGSERIAL    PRIMARY KEY,
                       title               VARCHAR(255) NOT NULL,
                       program_id          BIGINT       NOT NULL
                           REFERENCES programs(id),
                       deadline            TIMESTAMP    NOT NULL,
                       status              VARCHAR(20)  NOT NULL
                           DEFAULT 'OPEN',
                       evaluation_criteria TEXT,
                       created_at          TIMESTAMP    NOT NULL
                           DEFAULT NOW()
);

CREATE INDEX idx_calls_program_id
    ON calls(program_id);
CREATE INDEX idx_calls_status
    ON calls(status);