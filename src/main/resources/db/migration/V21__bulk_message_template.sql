INSERT INTO email_templates (type, subject, body, variables)
VALUES (
    'BULK_MESSAGE',
    '{{subject}}',
    '{{body}}

--
This message was sent by the NTI administrator. Please do not reply to this email.',
    'subject,body'
);
