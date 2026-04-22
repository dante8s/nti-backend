CREATE TABLE users (

                       id                   BIGSERIAL    PRIMARY KEY,
                       name                 VARCHAR(255) NOT NULL,
                       email                VARCHAR(255) NOT NULL UNIQUE,
                       password             VARCHAR(255) NOT NULL,
                       enabled              BOOLEAN      NOT NULL DEFAULT FALSE,
                       email_verified       BOOLEAN      NOT NULL DEFAULT FALSE,
                       verification_token   VARCHAR(255),
                       reset_password_token VARCHAR(255),
                       reset_token_expiry   TIMESTAMP,
                       onboarding_completed BOOLEAN      NOT NULL DEFAULT FALSE,
                       account_status       VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
                       created_at           TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE user_roles (
                            user_id BIGINT      NOT NULL,
                            role    VARCHAR(50) NOT NULL,
                            PRIMARY KEY (user_id, role),
                            CONSTRAINT fk_user_roles_user
                                FOREIGN KEY (user_id)
                                    REFERENCES users(id)
                                    ON DELETE CASCADE
);


CREATE INDEX idx_users_email
    ON users(email);

CREATE INDEX idx_users_verification_token
    ON users(verification_token);

CREATE INDEX idx_users_reset_token
    ON users(reset_password_token);

CREATE INDEX idx_users_account_status
    ON users(account_status);
