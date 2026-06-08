ALTER TABLE users
    ADD COLUMN gdpr_consented_at TIMESTAMP     NULL,
    ADD COLUMN gdpr_consent_ip   VARCHAR(45)   NULL;
