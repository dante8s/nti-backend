package com.nti.nti_backend.gdpr;

import com.nti.nti_backend.application.ApplicationRepository;
import com.nti.nti_backend.audit.AuditRepository;
import com.nti.nti_backend.notification.NotificationRepository;
import com.nti.nti_backend.studentProfile.StudentProfileRepository;
import com.nti.nti_backend.exception.AppException;
import com.nti.nti_backend.user.AccountStatus;
import com.nti.nti_backend.user.User;
import com.nti.nti_backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class GdprService {

    private final UserRepository            userRepository;
    private final StudentProfileRepository  studentProfileRepository;
    private final ApplicationRepository     applicationRepository;
    private final NotificationRepository    notificationRepository;
    private final AuditRepository           auditRepository;
    private final PasswordEncoder           passwordEncoder;

    // ── Export ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> exportData(User user) {
        Map<String, Object> data = new LinkedHashMap<>();

        data.put("account", Map.of(
                "id",              user.getId(),
                "name",            user.getName(),
                "email",           user.getEmail(),
                "roles",           user.getRoles().stream().map(Enum::name).toList(),
                "createdAt",       user.getCreatedAt(),
                "gdprConsentedAt", user.getGdprConsentedAt() != null
                                       ? user.getGdprConsentedAt().toString() : null
        ));

        studentProfileRepository.findByUser_Id(user.getId()).ifPresent(sp ->
                data.put("studentProfile", Map.of(
                        "studyProgram", sp.getStudyProgram() != null ? sp.getStudyProgram() : "",
                        "yearOfStudy",  sp.getYearOfStudy() != null ? sp.getYearOfStudy()  : "",
                        "skills",       sp.getSkills()      != null ? sp.getSkills()        : "",
                        "bio",          sp.getBio()         != null ? sp.getBio()           : ""
                ))
        );

        var applications = applicationRepository.findByApplicantIdWithDetails(user.getId())
                .stream()
                .map(a -> Map.of(
                        "id",        a.getId(),
                        "call",      a.getCall().getTitle(),
                        "program",   a.getCall().getProgram().getName(),
                        "status",    a.getStatus().name(),
                        "createdAt", a.getCreatedAt()
                ))
                .toList();
        data.put("applications", applications);

        var notifications = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(n -> Map.of(
                        "type",      n.getType().name(),
                        "title",     n.getTitle(),
                        "createdAt", n.getCreatedAt()
                ))
                .toList();
        data.put("notifications", notifications);

        var auditEvents = auditRepository.findByActor_IdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(e -> Map.of(
                        "action",    e.getAction(),
                        "entity",    e.getEntityType(),
                        "createdAt", e.getCreatedAt()
                ))
                .toList();
        data.put("auditEvents", auditEvents);

        data.put("exportedAt", LocalDateTime.now().toString());
        return data;
    }

    // ── Anonymize ─────────────────────────────────────────────────────────────

    @Transactional
    public void anonymize(User user, String passwordConfirm) {
        if (!passwordEncoder.matches(passwordConfirm, user.getPassword())) {
            throw AppException.badRequest("Невірний пароль");
        }

        user.setName("Видалений користувач");
        user.setEmail("deleted_" + user.getId() + "@nti.removed");
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setVerificationToken(null);
        user.setInviteToken(null);
        user.setResetPasswordToken(null);
        user.setResetTokenExpiry(null);
        user.setEnabled(false);
        user.setAccountStatus(AccountStatus.ANONYMIZED);
        user.setGdprConsentedAt(null);
        user.setGdprConsentIp(null);
        userRepository.save(user);

        notificationRepository.deleteAll(
                notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
        );

        studentProfileRepository.findByUser_Id(user.getId()).ifPresent(sp -> {
            sp.setBio(null);
            sp.setSkills(null);
            sp.setAvatarFilePath(null);
            sp.setAvatarOriginalName(null);
            sp.setCvFilePath(null);
            sp.setCvOriginalName(null);
            studentProfileRepository.save(sp);
        });
    }
}
