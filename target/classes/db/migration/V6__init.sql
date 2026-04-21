CREATE TABLE applications (
                              id            BIGSERIAL   PRIMARY KEY,
                              call_id       BIGINT      NOT NULL
                                  REFERENCES calls(id),
                              applicant_id  BIGINT      NOT NULL
                                  REFERENCES users(id),
                              status        VARCHAR(30) NOT NULL
                                                                 DEFAULT 'DRAFT',
                              admin_comment TEXT,
                              created_at    TIMESTAMP   NOT NULL DEFAULT NOW(),
                              updated_at    TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE TABLE application_documents (
                                       id             BIGSERIAL    PRIMARY KEY,
                                       application_id BIGINT       NOT NULL
                                           REFERENCES applications(id),
                                       file_name      VARCHAR(255) NOT NULL,
                                       file_path      VARCHAR(500) NOT NULL,
                                       file_type      VARCHAR(50)  NOT NULL,
                                       uploaded_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_apps_applicant
    ON applications(applicant_id);
CREATE INDEX idx_apps_status
    ON applications(status);
CREATE INDEX idx_apps_call
    ON applications(call_id);