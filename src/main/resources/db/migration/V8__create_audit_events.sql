CREATE TABLE audit_events (
                              id          BIGSERIAL    PRIMARY KEY,
                              actor_id    BIGINT       REFERENCES users(id),
                              action      VARCHAR(100) NOT NULL,
                              entity_type VARCHAR(50)  NOT NULL,
                              entity_id   BIGINT,
                              description TEXT,
                              created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_entity
    ON audit_events(entity_type, entity_id);