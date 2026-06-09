INSERT INTO email_templates (type, subject, body, variables)
VALUES (
    'BULK_MESSAGE',
    '{{subject}}',
    '{{body}}

--
Це повідомлення надіслано адміністратором NTI. Не відповідайте на цей лист.',
    'subject,body'
);
