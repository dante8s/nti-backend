UPDATE email_templates
SET body = '{{body}}

--
Це повідомлення надіслано адміністратором NTI. Не відповідайте на цей лист.'
WHERE type = 'BULK_MESSAGE';
