
CREATE TABLE users (
                       id                    BIGSERIAL PRIMARY KEY,
                       name                  VARCHAR(255) NOT NULL,
                       email                 VARCHAR(255) NOT NULL UNIQUE,
                       password              VARCHAR(255) NOT NULL,
                       role                  VARCHAR(50)  NOT NULL,
                       enabled               BOOLEAN      NOT NULL DEFAULT TRUE,
                       email_verified        BOOLEAN      NOT NULL DEFAULT FALSE,
                       verification_token    VARCHAR(255),
                       reset_password_token  VARCHAR(255),
                       reset_token_expiry    TIMESTAMP,
                       onboarding_completed  BOOLEAN      NOT NULL DEFAULT FALSE,
                       created_at            TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Індекс для швидкого пошуку по email
CREATE INDEX idx_users_email
    ON users(email);

-- Індекс для пошуку по verification_token
CREATE INDEX idx_users_verification_token
    ON users(verification_token);

-- Індекс для пошуку по reset_password_token
CREATE INDEX idx_users_reset_token
    ON users(reset_password_token);