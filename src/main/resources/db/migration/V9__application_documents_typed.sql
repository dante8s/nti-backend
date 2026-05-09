-- Додаємо тип документу до таблиці
ALTER TABLE application_documents
    ADD COLUMN document_type VARCHAR(50) NOT NULL DEFAULT 'OTHER';

-- Додаємо індекс
CREATE INDEX idx_app_docs_type
    ON application_documents(document_type);