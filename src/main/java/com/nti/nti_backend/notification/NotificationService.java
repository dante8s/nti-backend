package com.nti.nti_backend.notification;

import com.nti.nti_backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repository;

    // ── Create ────────────────────────────────────────────────────────────────

    public void create(User user, NotificationType type, String title, String message, String link) {
        repository.save(Notification.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .link(link)
                .read(false)
                .build());
    }

    // ── Convenience factories ─────────────────────────────────────────────────

    public void notifyApplicationStatusChanged(User user, String status,
                                               String comment, Long applicationId) {
        String title = "Статус заявки змінено";
        String msg   = "Ваша заявка перейшла в статус: " + humanStatus(status)
                + (comment != null && !comment.isBlank() ? "\nКоментар: " + comment : "");
        create(user, NotificationType.APPLICATION_STATUS_CHANGED,
                title, msg, "/app/applications/" + applicationId);
    }

    public void notifyMentorAssigned(User user, String mentorName, Long applicationId) {
        create(user, NotificationType.MENTOR_ASSIGNED,
                "Ментор призначено",
                "До вашого проєкту призначено ментора: " + mentorName,
                "/app/applications/" + applicationId);
    }

    public void notifyMilestoneStatusChanged(User user, String milestoneTitle,
                                             String newStatus, String milestoneId) {
        create(user, NotificationType.MILESTONE_STATUS_CHANGED,
                "Статус етапу змінено",
                "Етап «" + milestoneTitle + "» перейшов у статус: " + newStatus,
                "/app/applications");
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    public List<NotificationDTO> getForUser(Long userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::toDTO).toList();
    }

    public long getUnreadCount(Long userId) {
        return repository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markRead(Long notificationId, Long userId) {
        repository.findById(notificationId).ifPresent(n -> {
            if (n.getUser().getId().equals(userId)) {
                n.setRead(true);
                repository.save(n);
            }
        });
    }

    @Transactional
    public void markAllRead(Long userId) {
        repository.markAllReadByUserId(userId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static final Map<String, String> STATUS_LABELS = Map.ofEntries(
            Map.entry("DRAFT",              "Чернетка"),
            Map.entry("SUBMITTED",          "Подано"),
            Map.entry("FORMALLY_VERIFIED",  "Офіційно підтверджено"),
            Map.entry("IN_REVIEW",          "На оцінюванні"),
            Map.entry("NEEDS_REVISION",     "Потрібні виправлення"),
            Map.entry("APPROVED",           "Схвалено"),
            Map.entry("REJECTED",           "Відхилено"),
            Map.entry("ONBOARDING",         "Адаптація"),
            Map.entry("ACTIVE",             "Активний проєкт"),
            Map.entry("SUSPENDED",          "Відсторонено"),
            Map.entry("ARCHIVED",           "Архівовано")
    );

    private String humanStatus(String status) {
        return STATUS_LABELS.getOrDefault(status, status);
    }

    private NotificationDTO toDTO(Notification n) {
        return new NotificationDTO(
                n.getId(), n.getType().name(),
                n.getTitle(), n.getMessage(),
                n.getLink(), n.isRead(), n.getCreatedAt()
        );
    }
}
