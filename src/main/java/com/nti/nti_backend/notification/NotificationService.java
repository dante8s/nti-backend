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
        String title = "Application status changed";
        String msg   = "Your application status has changed to: " + humanStatus(status)
                + (comment != null && !comment.isBlank() ? "\nComment: " + comment : "");
        create(user, NotificationType.APPLICATION_STATUS_CHANGED,
                title, msg, "/app/applications/" + applicationId);
    }

    public void notifyMentorAssigned(User user, String mentorName, Long applicationId) {
        create(user, NotificationType.MENTOR_ASSIGNED,
                "Mentor assigned",
                "A mentor has been assigned to your project: " + mentorName,
                "/app/applications/" + applicationId);
    }

    public void notifyMilestoneStatusChanged(User user, String milestoneTitle,
                                             String newStatus, String milestoneId) {
        create(user, NotificationType.MILESTONE_STATUS_CHANGED,
                "Milestone status changed",
                "Milestone \"" + milestoneTitle + "\" has changed to status: " + newStatus,
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
            Map.entry("DRAFT",              "Draft"),
            Map.entry("SUBMITTED",          "Submitted"),
            Map.entry("FORMALLY_VERIFIED",  "Formally verified"),
            Map.entry("IN_REVIEW",          "Under review"),
            Map.entry("NEEDS_REVISION",     "Needs revision"),
            Map.entry("APPROVED",           "Approved"),
            Map.entry("REJECTED",           "Rejected"),
            Map.entry("ONBOARDING",         "Onboarding"),
            Map.entry("ACTIVE",             "Active project"),
            Map.entry("SUSPENDED",          "Suspended"),
            Map.entry("ARCHIVED",           "Archived")
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
