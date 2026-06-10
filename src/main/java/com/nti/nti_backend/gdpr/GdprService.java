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

        Map<String, Object> account = new LinkedHashMap<>();
        account.put("id",              user.getId());
        account.put("name",            user.getName());
        account.put("email",           user.getEmail());
        account.put("roles",           user.getRoles().stream().map(Enum::name).toList());
        account.put("createdAt",       user.getCreatedAt());
        account.put("gdprConsentedAt", user.getGdprConsentedAt() != null
                                           ? user.getGdprConsentedAt().toString() : null);
        data.put("account", account);

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
                .map(a -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id",        a.getId());
                    m.put("call",      a.getCall().getTitle());
                    m.put("program",   a.getCall().getProgram().getName());
                    m.put("status",    a.getStatus().name());
                    m.put("createdAt", a.getCreatedAt());
                    return m;
                })
                .toList();
        data.put("applications", applications);

        var notifications = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(n -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("type",      n.getType().name());
                    m.put("title",     n.getTitle() != null ? n.getTitle() : "");
                    m.put("createdAt", n.getCreatedAt());
                    return m;
                })
                .toList();
        data.put("notifications", notifications);

        var auditEvents = auditRepository.findByActor_IdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("action",    e.getAction() != null ? e.getAction() : "");
                    m.put("entity",    e.getEntityType() != null ? e.getEntityType() : "");
                    m.put("createdAt", e.getCreatedAt());
                    return m;
                })
                .toList();
        data.put("auditEvents", auditEvents);

        data.put("exportedAt", LocalDateTime.now().toString());
        return data;
    }

    // ── Anonymize ─────────────────────────────────────────────────────────────

    @Transactional
    public void anonymize(User user, String passwordConfirm) {
        if (!passwordEncoder.matches(passwordConfirm, user.getPassword())) {
            throw AppException.badRequest("Invalid password");
        }

        user.setName("Deleted user");
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
