INSERT INTO email_templates (type, subject, body, variables)
VALUES
(
    'COMPLETION_REJECTED',
    'NTI — Project completion request rejected',
    'Hello, {{name}}!

The administrator has rejected your request to complete the project "{{projectName}}".

The project remains active. If you have any questions, please contact the administrator.

NTI Team',
    'name,projectName'
),
(
    'TEAM_INVITE_UNREGISTERED',
    'NTI — Invitation to team "{{teamName}}"',
    'Hello!

You have been invited to join the team "{{teamName}}" on the NTI platform.

To participate, follow the link:

{{link}}

You will need to provide your name and password.
After registration you will be able to accept or decline the invitation in your personal account.

The link is valid for 7 days.

NTI Team',
    'teamName,link'
);
