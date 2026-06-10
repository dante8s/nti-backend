UPDATE email_templates
SET body = '{{body}}

--
This message was sent by the NTI administrator. Please do not reply to this email.'
WHERE type = 'BULK_MESSAGE';
