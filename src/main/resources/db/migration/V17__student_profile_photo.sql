-- Фото профілю студента (опційно)
ALTER TABLE student_profiles
    ADD COLUMN avatar_file_path VARCHAR(512),
    ADD COLUMN avatar_original_name VARCHAR(255),
    ADD COLUMN avatar_uploaded_at TIMESTAMP;
