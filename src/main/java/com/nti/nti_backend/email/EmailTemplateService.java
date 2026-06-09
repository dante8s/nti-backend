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
                .orElseThrow(() -> new RuntimeException("Шаблон не знайдено: " + type));
    }

    public EmailTemplateDTO update(Long id, UpdateEmailTemplateRequest req) {
        EmailTemplate tpl = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Шаблон не знайдено"));
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
                .orElseThrow(() -> new RuntimeException("Шаблон не знайдено: " + type));
        String subject = replace(tpl.getSubject(), vars);
        String body    = replace(tpl.getBody(), vars);
        return new String[]{subject, body};
    }

    // ── Seeder ────────────────────────────────────────────────────────────────

    @PostConstruct
    void seed() {
        seed(EmailTemplateType.VERIFICATION,
                "NTI — Підтвердіть вашу email адресу",
                """
                Вітаємо у NTI!

                Для підтвердження вашої email адреси перейдіть за посиланням:

                {{link}}

                Посилання дійсне 24 години.

                Якщо ви не реєструвались — проігноруйте цей лист.

                Команда NTI""",
                "{{link}}");

        seed(EmailTemplateType.RESET_PASSWORD,
                "NTI — Скидання пароля",
                """
                Ви запросили скидання пароля.

                Перейдіть за посиланням щоб встановити новий пароль:

                {{link}}

                Посилання дійсне 1 годину.

                Якщо ви не запитували скидання — проігноруйте цей лист.

                Команда NTI""",
                "{{link}}");

        seed(EmailTemplateType.WELCOME,
                "NTI — Ласкаво просимо!",
                """
                Вітаємо, {{name}}!

                Ваш акаунт успішно підтверджено.

                Тепер заповніть ваш профіль щоб подати заявку на програму:
                {{link}}

                Команда NTI""",
                "{{name}},{{link}}");

        seed(EmailTemplateType.APPLICATION_STATUS_CHANGED,
                "NTI — Статус вашої заявки змінено",
                """
                Вітаємо, {{name}}!

                Статус вашої заявки змінено на: {{status}}

                {{comment}}

                Деталі у вашому кабінеті:
                {{link}}

                Команда NTI""",
                "{{name}},{{status}},{{comment}},{{link}}");

        seed(EmailTemplateType.NEW_USER_NOTIFICATION,
                "NTI — Новий користувач очікує схвалення",
                """
                Новий користувач зареєструвався:

                Ім'я: {{name}}
                Email: {{email}}
                Ролі: {{roles}}

                Перейдіть до панелі адміна щоб схвалити:
                {{link}}""",
                "{{name}},{{email}},{{roles}},{{link}}");

        seed(EmailTemplateType.MENTOR_INVITE,
                "NTI — Запрошення стати ментором",
                """
                Вітаємо!

                Адміністратор NTI запросив вас стати ментором.

                Для завершення реєстрації перейдіть за посиланням:

                {{link}}

                Вам потрібно буде вказати ваше ім'я та пароль.
                Посилання дійсне 7 днів.

                Команда NTI""",
                "{{link}}");

        seed(EmailTemplateType.ORG_MEMBER_INVITE,
                "NTI — Запрошення до організації {{orgName}}",
                """
                Вітаємо!

                Вас запросили приєднатися до організації «{{orgName}}» на платформі NTI.

                Для завершення реєстрації перейдіть за посиланням:

                {{link}}

                Посилання дійсне 7 днів.

                Команда NTI""",
                "{{orgName}},{{link}}");

        seed(EmailTemplateType.ACCOUNT_APPROVED,
                "NTI — Ваш акаунт схвалено!",
                """
                Вітаємо, {{name}}!

                Ваш акаунт схвалено адміністратором.

                Тепер ви можете увійти в систему:
                {{link}}

                Команда NTI""",
                "{{name}},{{link}}");

        seed(EmailTemplateType.ACCOUNT_REJECTED,
                "NTI — Акаунт не схвалено",
                """
                Вітаємо, {{name}}!

                На жаль, ваш акаунт не схвалено.

                Причина: {{reason}}

                Якщо маєте питання — зверніться до нас: {{supportEmail}}

                Команда NTI""",
                "{{name}},{{reason}},{{supportEmail}}");

        seed(EmailTemplateType.ACCOUNT_SUSPENDED,
                "NTI — Акаунт заблоковано",
                """
                Вітаємо, {{name}}!

                Ваш акаунт було заблоковано.

                Причина: {{reason}}

                Якщо маєте питання — зверніться до нас: {{supportEmail}}

                Команда NTI""",
                "{{name}},{{reason}},{{supportEmail}}");

        seed(EmailTemplateType.MENTOR_ASSIGNED,
                "NTI — До вашого проєкту призначено ментора",
                """
                Вітаємо, {{name}}!

                До вашого проєкту призначено ментора: {{mentorName}}

                Ментор зв'яжеться з вами найближчим часом для узгодження першої консультації.

                Деталі у вашому кабінеті:
                {{link}}

                Команда NTI""",
                "{{name}},{{mentorName}},{{link}}");

        seed(EmailTemplateType.DEADLINE_REMINDER,
                "NTI — Нагадування: дедлайн через {{daysLeft}} днів",
                """
                Вітаємо, {{name}}!

                Нагадуємо, що дедлайн для подачі заявки на «{{callTitle}}» спливає через {{daysLeft}} днів.

                Перейдіть до вашої заявки:
                {{link}}

                Команда NTI""",
                "{{name}},{{callTitle}},{{daysLeft}},{{link}}");

        seed(EmailTemplateType.PROJECT_CLOSED,
                "NTI — Проєкт завершено",
                """
                Вітаємо, {{name}}!

                Ваш проєкт «{{projectTitle}}» успішно завершено та переведено в архів.

                Дякуємо за участь у програмі NTI!

                Команда NTI""",
                "{{name}},{{projectTitle}}");

        seed(EmailTemplateType.BULK_MESSAGE,
                "{{subject}}",
                """
                {{body}}

                --
                Це повідомлення надіслано адміністратором NTI. Не відповідайте на цей лист.""",
                "subject,body");

        seed(EmailTemplateType.COMPLETION_REJECTED,
                "NTI — Запит на завершення проекту відхилено",
                """
                Вітаємо, {{name}}!

                Адміністратор відхилив ваш запит на завершення проекту «{{projectName}}».

                Проект залишається активним. Якщо у вас є питання — зверніться до адміністратора.

                Команда NTI""",
                "name,projectName");

        seed(EmailTemplateType.TEAM_INVITE_UNREGISTERED,
                "NTI — Запрошення до команди «{{teamName}}»",
                """
                Вітаємо!

                Вас запрошують до команди «{{teamName}}» на платформі NTI.

                Для того щоб прийняти участь, перейдіть за посиланням:

                {{link}}

                Вам потрібно буде вказати ваше ім'я та пароль.
                Після реєстрації ви зможете прийняти або відхилити запрошення в особистому кабінеті.

                Посилання дійсне 7 днів.

                Команда NTI""",
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
