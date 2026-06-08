-- Снапшот учасників команди на момент подачі заявки
CREATE TABLE IF NOT EXISTS application_members (
    id BIGSERIAL PRIMARY KEY,
    application_id BIGINT NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL,
    email VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_application_members_application_id ON application_members(application_id);
CREATE INDEX IF NOT EXISTS idx_application_members_user_id ON application_members(user_id);

-- Новий статус COMPLETED для заявок (якщо використовується CHECK constraint)
-- PostgreSQL enum або рядкові значення — нічого додавати не треба,
-- бо статус зберігається як VARCHAR через @Enumerated(EnumType.STRING)
