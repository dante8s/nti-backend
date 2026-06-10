-- Student profile photo (optional)
ALTER TABLE student_profiles
    ADD COLUMN IF NOT EXISTS avatar_file_path VARCHAR(512),
    ADD COLUMN IF NOT EXISTS avatar_original_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS avatar_uploaded_at TIMESTAMP;