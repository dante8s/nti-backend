package com.nti.nti_backend.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender        mailSender;
    private final EmailTemplateService  templateService;
    private final TemplateEngine        templateEngine;

    @Value("${app.backend-url}")  private String backendUrl;
    @Value("${app.frontend-url}") private String frontendUrl;
    @Value("${app.mail.from}")    private String mailFrom;
    @Value("${app.mail.admin}")   private String adminEmail;

    // ── Core send ─────────────────────────────────────────────────────────────

    @Async
    public void send(String to, EmailTemplateType type, Map<String, String> vars) {
        try {
            String[] rendered = templateService.render(type, vars);
            String subject = rendered[0];
            String body    = rendered[1];

            Context ctx = new Context();
            ctx.setVariable("subject", subject);
            ctx.setVariable("body", body);
            String html = templateEngine.process("email/base", ctx);

            var message = mailSender.createMimeMessage();
            var helper  = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(mailFrom);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);

        } catch (Exception e) {
            log.warn("Failed to send email [{}] to {}: {}", type, to, e.getMessage());
        }
    }

    // ── Transactional emails ──────────────────────────────────────────────────

    @Async
    public void sendVerificationEmail(String to, String token) {
        send(to, EmailTemplateType.VERIFICATION, Map.of(
                "link", backendUrl + "/api/auth/verify?token=" + token
        ));
    }

    @Async
    public void sendResetPasswordEmail(String to, String token) {
        send(to, EmailTemplateType.RESET_PASSWORD, Map.of(
                "link", frontendUrl + "/reset-password?token=" + token
        ));
    }

    @Async
    public void sendWelcomeEmail(String to, String name) {
        send(to, EmailTemplateType.WELCOME, Map.of(
                "name", name,
                "link", frontendUrl + "/onboarding"
        ));
    }

    @Async
    public void sendApplicationStatusChanged(String to, String name,
                                             String status, String comment) {
        send(to, EmailTemplateType.APPLICATION_STATUS_CHANGED, Map.of(
                "name",    name,
                "status",  status,
                "comment", comment != null ? "Коментар: " + comment : "",
                "link",    frontendUrl + "/app/my-applications"
        ));
    }

    @Async
    public void sendNewUserNotification(String userName, String userEmail, String roles) {
        send(adminEmail, EmailTemplateType.NEW_USER_NOTIFICATION, Map.of(
                "name",  userName,
                "email", userEmail,
                "roles", roles,
                "link",  frontendUrl + "/app/admin/users"
        ));
    }

    @Async
    public void sendMentorInvite(String to, String token) {
        send(to, EmailTemplateType.MENTOR_INVITE, Map.of(
                "link", frontendUrl + "/complete-registration?token=" + token
        ));
    }

    @Async
    public void sendOrgMemberInvite(String to, String orgName, String token) {
        send(to, EmailTemplateType.ORG_MEMBER_INVITE, Map.of(
                "orgName", orgName,
                "link",    frontendUrl + "/complete-org-invite?token=" + token
        ));
    }

    @Async
    public void sendAccountApproved(String to, String name) {
        send(to, EmailTemplateType.ACCOUNT_APPROVED, Map.of(
                "name", name,
                "link", frontendUrl + "/login"
        ));
    }

    @Async
    public void sendAccountRejected(String to, String name, String reason) {
        send(to, EmailTemplateType.ACCOUNT_REJECTED, Map.of(
                "name",         name,
                "reason",       reason,
                "supportEmail", mailFrom
        ));
    }

    // Відхилення запиту на завершення проекту (тільки лідеру)
    @Async
    public void sendCompletionRejected(String to, String leaderName, String projectName) {
        send(
                to,
                "NTI — Запит на завершення проекту відхилено",
                "Вітаємо, " + leaderName + "!\n\n"
                        + "Адміністратор відхилив ваш запит на завершення проекту «" + projectName + "».\n\n"
                        + "Проект залишається активним. Якщо у вас є питання — зверніться до адміністратора.\n\n"
                        + "Команда NTI"
        );
    }
    // Запрошення незареєстрованого користувача до команди
    @Async
    public void sendTeamInviteToUnregistered(String to, String teamName, String token) {
        String link = frontendUrl + "/complete-team-invite?token=" + token;
        send(
                to,
                "NTI — Запрошення до команди «" + teamName + "»",
                "Вітаємо!\n\n"
                        + "Вас запрошують до команди «" + teamName + "» на платформі NTI.\n\n"
                        + "Для того щоб прийняти участь, перейдіть за посиланням:\n\n"
                        + link + "\n\n"
                        + "Вам потрібно буде вказати ваше ім'я та пароль.\n"
                        + "Після реєстрації ви зможете прийняти або відхилити запрошення в особистому кабінеті.\n\n"
                        + "Посилання дійсне 7 днів.\n\n"
                        + "Команда NTI"
        );
    }

    // Блокування акаунту
    @Async
    public void sendAccountSuspended(String to, String name, String reason) {
        send(to, EmailTemplateType.ACCOUNT_SUSPENDED, Map.of(
                "name",         name,
                "reason",       reason,
                "supportEmail", mailFrom
        ));
    }

    @Async
    public void sendMentorAssigned(String to, String name, String mentorName, Long applicationId) {
        send(to, EmailTemplateType.MENTOR_ASSIGNED, Map.of(
                "name",       name,
                "mentorName", mentorName,
                "link",       frontendUrl + "/app/applications/" + applicationId
        ));
    }

    @Async
    public void sendDeadlineReminder(String to, String name, String callTitle,
                                     String daysLeft, Long applicationId) {
        send(to, EmailTemplateType.DEADLINE_REMINDER, Map.of(
                "name",      name,
                "callTitle", callTitle,
                "daysLeft",  daysLeft,
                "link",      frontendUrl + "/app/applications/" + applicationId
        ));
    }

    @Async
    public void sendProjectClosed(String to, String name, String projectTitle) {
        send(to, EmailTemplateType.PROJECT_CLOSED, Map.of(
                "name",         name,
                "projectTitle", projectTitle
        ));
    }

    @Async
    public void sendBulkMessage(String to, String subject, String body) {
        send(to, EmailTemplateType.BULK_MESSAGE, Map.of(
                "subject", subject,
                "body",    body
        ));
    }
}
