package com.nti.nti_backend.email;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.backend-url}")
    private String backendUrl;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.mail.from}")
    private String mailFrom;

    @Value("${app.mail.admin}")
    private String adminEmail;

    // Базовий метод відправки
    private void send(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }

    // Підтвердження email після реєстрації
    public void sendVerificationEmail(String to, String token) {
        String link = backendUrl + "/api/auth/verify?token=" + token;
        send(
                to,
                "NTI — Підтвердіть вашу email адресу",
                "Вітаємо у NTI!\n\n"
                        + "Для підтвердження вашої email адреси перейдіть за посиланням:\n\n"
                        + link + "\n\n"
                        + "Посилання дійсне 24 години.\n\n"
                        + "Якщо ви не реєструвались — проігноруйте цей лист."
        );
    }

    // Скидання пароля
    public void sendResetPasswordEmail(String to, String token) {
        String link = frontendUrl + "/reset-password?token=" + token;
        send(
                to,
                "NTI — Скидання пароля",
                "Ви запросили скидання пароля.\n\n"
                        + "Перейдіть за посиланням щоб встановити новий пароль:\n\n"
                        + link + "\n\n"
                        + "Посилання дійсне 1 годину.\n\n"
                        + "Якщо ви не запитували скидання — проігноруйте цей лист."
        );
    }

    // Вітальний лист після підтвердження email
    public void sendWelcomeEmail(String to, String name) {
        send(
                to,
                "NTI — Ласкаво просимо!",
                "Вітаємо, " + name + "!\n\n"
                        + "Ваш акаунт успішно підтверджено.\n\n"
                        + "Тепер заповніть ваш профіль щоб подати заявку на програму.\n\n"
                        + frontendUrl + "/onboarding\n\n"
                        + "Команда NTI"
        );
    }

    // Нотифікація про зміну статусу заявки
    public void sendApplicationStatusChanged(
            String to,
            String applicantName,
            String newStatus,
            String comment) {
        send(
                to,
                "NTI — Статус вашої заявки змінено",
                "Вітаємо, " + applicantName + "!\n\n"
                        + "Статус вашої заявки змінено на: " + newStatus + "\n\n"
                        + (comment != null && !comment.isBlank()
                        ? "Коментар: " + comment + "\n\n"
                        : "")
                        + "Деталі у вашому кабінеті:\n"
                        + frontendUrl + "/student/applications\n\n"
                        + "Команда NTI"
        );
    }

    // Сповіщення адмінів про нового юзера
    public void sendNewUserNotification(
            String userName, String userEmail, String roles) {
        send(
                adminEmail,
                "NTI — Новий користувач очікує схвалення",
                "Новий користувач зареєструвався:\n\n"
                        + "Ім'я: " + userName + "\n"
                        + "Email: " + userEmail + "\n"
                        + "Ролі: " + roles + "\n\n"
                        + "Перейдіть до панелі адміна щоб схвалити:\n"
                        + frontendUrl + "/admin/users"
        );
    }

    // Схвалення акаунту
    public void sendAccountApproved(String to, String name) {
        send(
                to,
                "NTI — Ваш акаунт схвалено!",
                "Вітаємо, " + name + "!\n\n"
                        + "Ваш акаунт схвалено адміністратором.\n\n"
                        + "Тепер ви можете увійти в систему:\n"
                        + frontendUrl + "/login\n\n"
                        + "Команда NTI"
        );
    }

    // Відхилення акаунту
    public void sendAccountRejected(String to, String name, String reason) {
        send(
                to,
                "NTI — Акаунт не схвалено",
                "Вітаємо, " + name + "!\n\n"
                        + "На жаль, ваш акаунт не схвалено.\n\n"
                        + "Причина: " + reason + "\n\n"
                        + "Якщо маєте питання — зверніться до нас:\n"
                        + mailFrom + "\n\n"
                        + "Команда NTI"
        );
    }

    // Блокування акаунту
    public void sendAccountSuspended(String to, String name, String reason) {
        send(
                to,
                "NTI — Акаунт заблоковано",
                "Вітаємо, " + name + "!\n\n"
                        + "Ваш акаунт було заблоковано.\n\n"
                        + "Причина: " + reason + "\n\n"
                        + "Якщо маєте питання — зверніться до нас:\n"
                        + mailFrom + "\n\n"
                        + "Команда NTI"
        );
    }
}