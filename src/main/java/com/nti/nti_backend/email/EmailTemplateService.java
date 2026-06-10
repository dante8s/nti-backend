package com.nti.nti_backend.email;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmailTemplateService {

    private final EmailTemplateRepository repository;

    // ── Public API ────────────────────────────────────────────────────────────

    public List<EmailTemplateDTO> getAll() {
        return repository.findAll().stream().map(this::toDTO).toList();
    }

    public EmailTemplateDTO getByType(EmailTemplateType type) {
        return repository.findByType(type)
                .map(this::toDTO)
                .orElseThrow(() -> new RuntimeException("Template not found: " + type));
    }

    public EmailTemplateDTO update(Long id, UpdateEmailTemplateRequest req) {
        EmailTemplate tpl = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Template not found"));
        tpl.setSubject(req.subject());
        tpl.setBody(req.body());
        return toDTO(repository.save(tpl));
    }

    /**
     * Renders subject and body by replacing {{variable}} placeholders with provided values.
     * Returns rendered [subject, body] pair.
     */
    public String[] render(EmailTemplateType type, Map<String, String> vars) {
        EmailTemplate tpl = repository.findByType(type)
                .orElseThrow(() -> new RuntimeException("Template not found: " + type));
        String subject = replace(tpl.getSubject(), vars);
        String body    = replace(tpl.getBody(), vars);
        return new String[]{subject, body};
    }

    // ── Seeder ────────────────────────────────────────────────────────────────

    @PostConstruct
    void seed() {
        seed(EmailTemplateType.VERIFICATION,
                "NTI — Please verify your email address",
                """
                Welcome to NTI!

                To verify your email address, please click the link below:

                {{link}}

                The link is valid for 24 hours.

                If you did not register, please ignore this email.

                NTI Team""",
                "{{link}}");

        seed(EmailTemplateType.RESET_PASSWORD,
                "NTI — Password reset",
                """
                You requested a password reset.

                Click the link below to set a new password:

                {{link}}

                The link is valid for 1 hour.

                If you did not request a reset, please ignore this email.

                NTI Team""",
                "{{link}}");

        seed(EmailTemplateType.WELCOME,
                "NTI — Welcome!",
                """
                Hello, {{name}}!

                Your account has been successfully verified.

                Please complete your profile to apply for a program:
                {{link}}

                NTI Team""",
                "{{name}},{{link}}");

        seed(EmailTemplateType.APPLICATION_STATUS_CHANGED,
                "NTI — Your application status has been updated",
                """
                Hello, {{name}}!

                Your application status has been changed to: {{status}}

                {{comment}}

                View details in your account:
                {{link}}

                NTI Team""",
                "{{name}},{{status}},{{comment}},{{link}}");

        seed(EmailTemplateType.NEW_USER_NOTIFICATION,
                "NTI — A new user is awaiting approval",
                """
                A new user has registered:

                Name: {{name}}
                Email: {{email}}
                Roles: {{roles}}

                Go to the admin panel to approve:
                {{link}}""",
                "{{name}},{{email}},{{roles}},{{link}}");

        seed(EmailTemplateType.MENTOR_INVITE,
                "NTI — Invitation to become a mentor",
                """
                Hello!

                The NTI administrator has invited you to become a mentor.

                To complete your registration, please click the link below:

                {{link}}

                You will need to provide your name and set a password.
                The link is valid for 7 days.

                NTI Team""",
                "{{link}}");

        seed(EmailTemplateType.ORG_MEMBER_INVITE,
                "NTI — Invitation to join organization {{orgName}}",
                """
                Hello!

                You have been invited to join the organization "{{orgName}}" on the NTI platform.

                To complete your registration, please click the link below:

                {{link}}

                The link is valid for 7 days.

                NTI Team""",
                "{{orgName}},{{link}}");

        seed(EmailTemplateType.ACCOUNT_APPROVED,
                "NTI — Your account has been approved!",
                """
                Hello, {{name}}!

                Your account has been approved by the administrator.

                You can now log in:
                {{link}}

                NTI Team""",
                "{{name}},{{link}}");

        seed(EmailTemplateType.ACCOUNT_REJECTED,
                "NTI — Account not approved",
                """
                Hello, {{name}}!

                Unfortunately, your account has not been approved.

                Reason: {{reason}}

                If you have questions, please contact us: {{supportEmail}}

                NTI Team""",
                "{{name}},{{reason}},{{supportEmail}}");

        seed(EmailTemplateType.ACCOUNT_SUSPENDED,
                "NTI — Account suspended",
                """
                Hello, {{name}}!

                Your account has been suspended.

                Reason: {{reason}}

                If you have questions, please contact us: {{supportEmail}}

                NTI Team""",
                "{{name}},{{reason}},{{supportEmail}}");

        seed(EmailTemplateType.MENTOR_ASSIGNED,
                "NTI — A mentor has been assigned to your project",
                """
                Hello, {{name}}!

                A mentor has been assigned to your project: {{mentorName}}

                The mentor will contact you shortly to schedule the first consultation.

                View details in your account:
                {{link}}

                NTI Team""",
                "{{name}},{{mentorName}},{{link}}");

        seed(EmailTemplateType.DEADLINE_REMINDER,
                "NTI — Reminder: deadline in {{daysLeft}} days",
                """
                Hello, {{name}}!

                This is a reminder that the submission deadline for "{{callTitle}}" is in {{daysLeft}} days.

                Go to your application:
                {{link}}

                NTI Team""",
                "{{name}},{{callTitle}},{{daysLeft}},{{link}}");

        seed(EmailTemplateType.PROJECT_CLOSED,
                "NTI — Project completed",
                """
                Hello, {{name}}!

                Your project "{{projectTitle}}" has been successfully completed and archived.

                Thank you for participating in the NTI program!

                NTI Team""",
                "{{name}},{{projectTitle}}");

        seed(EmailTemplateType.BULK_MESSAGE,
                "{{subject}}",
                """
                {{body}}

                --
                This message was sent by the NTI administrator. Please do not reply to this email.""",
                "subject,body");

        seed(EmailTemplateType.COMPLETION_REJECTED,
                "NTI — Project completion request rejected",
                """
                Hello, {{name}}!

                The administrator has rejected your project completion request for "{{projectName}}".

                The project remains active. If you have any questions, please contact the administrator.

                NTI Team""",
                "name,projectName");

        seed(EmailTemplateType.TEAM_INVITE_UNREGISTERED,
                "NTI — Invitation to join team \"{{teamName}}\"",
                """
                Hello!

                You have been invited to join the team "{{teamName}}" on the NTI platform.

                To participate, please click the link below:

                {{link}}

                You will need to provide your name and set a password.
                After registration, you can accept or decline the invitation in your personal account.

                The link is valid for 7 days.

                NTI Team""",
                "teamName,link");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void seed(EmailTemplateType type, String subject, String body, String variables) {
        if (!repository.existsByType(type)) {
            repository.save(EmailTemplate.builder()
                    .type(type)
                    .subject(subject)
                    .body(body)
                    .variables(variables)
                    .build());
        }
    }

    private String replace(String template, Map<String, String> vars) {
        if (vars == null || vars.isEmpty()) return template;
        String result = template;
        for (Map.Entry<String, String> entry : vars.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() != null ? entry.getValue() : "";
            result = result.replace(placeholder, value);
        }
        return result;
    }

    private EmailTemplateDTO toDTO(EmailTemplate t) {
        List<String> vars = t.getVariables() == null || t.getVariables().isBlank()
                ? List.of()
                : Arrays.stream(t.getVariables().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isBlank())
                        .toList();
        return new EmailTemplateDTO(t.getId(), t.getType().name(),
                t.getSubject(), t.getBody(), vars, t.getUpdatedAt());
    }
}
