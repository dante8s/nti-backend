ALTER TABLE applications
    ADD COLUMN product_owner_id BIGINT NULL REFERENCES users(id) ON DELETE SET NULL;