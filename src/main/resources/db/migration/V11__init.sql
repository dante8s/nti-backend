-- V11: команди, профілі студентів, критерії та оцінки (PostgreSQL, узгоджено з JPA-сутностями)

CREATE TABLE teams (
    id             BIGSERIAL PRIMARY KEY,
    name           VARCHAR(150) NOT NULL,
    leader_id      BIGINT       NOT NULL
        REFERENCES users (id),
    max_capacity   INTEGER,
    description    TEXT,
    competencies   TEXT,
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_teams_leader_id ON teams (leader_id);

CREATE TABLE team_members (
    id             BIGSERIAL PRIMARY KEY,
    team_id        BIGINT       NOT NULL
        REFERENCES teams (id) ON DELETE CASCADE,
    user_id        BIGINT       NOT NULL
        REFERENCES users (id) ON DELETE CASCADE,
    role           VARCHAR(30)  NOT NULL,
    invite_status  VARCHAR(30)  NOT NULL,
    invited_at     TIMESTAMP,
    responded_at   TIMESTAMP,
    joined_at      TIMESTAMP,
    CONSTRAINT uq_team_members_team_user UNIQUE (team_id, user_id)
);

CREATE INDEX idx_team_members_team_id ON team_members (team_id);
CREATE INDEX idx_team_members_user_id ON team_members (user_id);

CREATE TABLE student_profiles (
    id                      BIGSERIAL PRIMARY KEY,
    user_id                 BIGINT       NOT NULL UNIQUE
        REFERENCES users (id) ON DELETE CASCADE,
    study_program           VARCHAR(150),
    year_of_study           INTEGER,
    skills                  TEXT,
    cv_file_path            VARCHAR(512),
    cv_original_name        VARCHAR(255),
    cv_uploaded_at          TIMESTAMP,
    has_repeated_subjects   BOOLEAN      NOT NULL DEFAULT FALSE,
    profile_average_grade   DOUBLE PRECISION,
    bio                     TEXT,
    profile_complete        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at              TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_student_profiles_user_id ON student_profiles (user_id);

CREATE TABLE criteria (
    id              BIGSERIAL PRIMARY KEY,
    call_id         BIGINT       NOT NULL
        REFERENCES calls (id) ON DELETE CASCADE,
    name            VARCHAR(150) NOT NULL,
    description     TEXT,
    weight_percent  INTEGER,
    max_score       DOUBLE PRECISION,
    sort_order      INTEGER
);

CREATE INDEX idx_criteria_call_id ON criteria (call_id);

CREATE TABLE evaluations (
    id               BIGSERIAL PRIMARY KEY,
    application_id   BIGINT       NOT NULL
        REFERENCES applications (id) ON DELETE CASCADE,
    evaluator_id     BIGINT       NOT NULL
        REFERENCES users (id),
    criteria_id      BIGINT       NOT NULL
        REFERENCES criteria (id) ON DELETE CASCADE,
    score            DOUBLE PRECISION NOT NULL,
    comment          TEXT,
    recommendation   VARCHAR(20),
    evaluated_at     TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_evaluations_application_id ON evaluations (application_id);
CREATE INDEX idx_evaluations_evaluator_id ON evaluations (evaluator_id);
CREATE INDEX idx_evaluations_criteria_id ON evaluations (criteria_id);

CREATE UNIQUE INDEX uq_evaluations_app_evaluator_criteria
    ON evaluations (application_id, evaluator_id, criteria_id);
