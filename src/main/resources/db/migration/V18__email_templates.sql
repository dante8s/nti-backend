CREATE TABLE email_templates (
    id           BIGSERIAL    PRIMARY KEY,
    type         VARCHAR(60)  NOT NULL UNIQUE,
    subject      VARCHAR(255) NOT NULL,
    body         TEXT         NOT NULL,
    variables    TEXT         NOT NULL DEFAULT '',
    updated_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_email_templates_type ON email_templates(type);
