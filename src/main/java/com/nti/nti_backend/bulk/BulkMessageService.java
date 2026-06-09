package com.nti.nti_backend.bulk;

import com.nti.nti_backend.application.ApplicationRepository;
import com.nti.nti_backend.application.ApplicationStatus;
import com.nti.nti_backend.audit.AuditService;
import com.nti.nti_backend.email.EmailService;
import com.nti.nti_backend.exception.AppException;
import com.nti.nti_backend.notification.NotificationService;
import com.nti.nti_backend.notification.NotificationType;
import com.nti.nti_backend.user.AccountStatus;
import com.nti.nti_backend.user.Role;
import com.nti.nti_backend.user.User;
import com.nti.nti_backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BulkMessageService {

    private final UserRepository        userRepository;
    private final ApplicationRepository applicationRepository;
    private final NotificationService   notificationService;
    private final EmailService          emailService;
    private final AuditService          auditService;

    /** Returns the list of recipients without sending anything (preview). */
    @Transactional(readOnly = true)
    public List<User> resolveRecipients(BulkMessageRequest req) {
        return switch (req.targetType()) {
            case "ALL" -> activeUsers();
            case "BY_ROLE" -> {
                if (req.roleFilter() == null || req.roleFilter().isBlank()) {
                    throw AppException.badRequest("roleFilter is required for BY_ROLE target");
                }
                Role role = parseRole(req.roleFilter());
                yield userRepository.findAllByRole(role).stream()
                        .filter(u -> u.getAccountStatus() == AccountStatus.APPROVED)
                        .collect(Collectors.toList());
            }
            case "BY_CALL" -> {
                if (req.callId() == null) {
                    throw AppException.badRequest("callId is required for BY_CALL target");
                }
                yield applicationRepository.findByCallIdWithApplicant(req.callId())
                        .stream()
                        .map(a -> a.getApplicant())
                        .filter(u -> u.getAccountStatus() == AccountStatus.APPROVED)
                        .distinct()
                        .collect(Collectors.toList());
            }
            case "BY_APPLICATION_STATUS" -> {
                if (req.applicationStatus() == null || req.applicationStatus().isBlank()) {
                    throw AppException.badRequest("applicationStatus is required for BY_APPLICATION_STATUS target");
                }
                ApplicationStatus status = parseAppStatus(req.applicationStatus());
                yield applicationRepository.findByStatus(status)
                        .stream()
                        .map(a -> a.getApplicant())
                        .filter(u -> u.getAccountStatus() == AccountStatus.APPROVED)
                        .distinct()
                        .collect(Collectors.toList());
            }
            default -> throw AppException.badRequest("Unknown targetType: " + req.targetType());
        };
    }

    /** Sends in-app notifications (+ optional emails) to all resolved recipients. */
    @Transactional
    public int send(BulkMessageRequest req, User actor) {
        List<User> recipients = resolveRecipients(req);

        for (User user : recipients) {
            notificationService.create(
                    user,
                    NotificationType.GENERAL,
                    req.subject(),
                    req.message(),
                    null
            );
            if (req.sendEmail()) {
                emailService.sendBulkMessage(user.getEmail(), req.subject(), req.message());
            }
        }

        auditService.log(actor, "BULK_MESSAGE_SENT", "USER", null,
                "Bulk message «" + req.subject() + "» → " + recipients.size()
                + " recipients, target=" + req.targetType()
                + (req.sendEmail() ? " (+email)" : " (in-app only)"));

        log.info("Bulk message sent: subject='{}' recipients={} email={} actor={}",
                req.subject(), recipients.size(), req.sendEmail(), actor.getEmail());

        return recipients.size();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private List<User> activeUsers() {
        return userRepository.findByAccountStatus(AccountStatus.APPROVED);
    }

    private Role parseRole(String role) {
        try {
            return Role.valueOf(role);
        } catch (IllegalArgumentException e) {
            throw AppException.badRequest("Unknown role: " + role);
        }
    }

    private ApplicationStatus parseAppStatus(String status) {
        try {
            return ApplicationStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw AppException.badRequest("Unknown application status: " + status);
        }
    }
}
