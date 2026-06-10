package com.nti.nti_backend.auth;

import com.nti.nti_backend.config.CacheNames;
import com.nti.nti_backend.audit.AuditService;
import com.nti.nti_backend.email.EmailService;
import com.nti.nti_backend.exception.AppException;
import com.nti.nti_backend.jwt.JwtUtil;
import com.nti.nti_backend.organization.repository.OrgMemberRepository;
import com.nti.nti_backend.recaptcha.RecaptchaService;
import com.nti.nti_backend.user.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import com.nti.nti_backend.audit.AuditService;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authManager;
    private final EmailService emailService;
    private final RecaptchaService recaptchaService;
    private final CacheManager cacheManager;
    private final OrgMemberRepository memberRepository;
    private final AuditService auditService;



    // Ролі які можна вибрати при реєстрації
    private static final Set<String> ALLOWED_ROLES =
            Set.of("STUDENT", "FIRM", "MENTOR");

    // -----------------------------------------------
    // РЕЄСТРАЦІЯ
    // -----------------------------------------------
    public String register(RegisterRequest request, String clientIp) {
        if (!recaptchaService.verify(request.captchaToken())) {
            throw AppException.badRequest("Captcha не пройдена. Спробуйте ще раз.");
        }

        if (!request.gdprConsent()) {
            throw AppException.badRequest("Потрібна згода на обробку даних");
        }

        if (!request.email().toLowerCase().endsWith("@student.ukf.sk")) {
            throw AppException.badRequest("Реєстрація дозволена лише для адрес @student.ukf.sk");
        }

        for (String role : request.roles()) {
            if (!ALLOWED_ROLES.contains(role)) {
                throw AppException.badRequest("Недозволена роль: " + role);
            }
        }

        if (userRepository.existsByEmail(request.email())) {
            throw AppException.conflict("Email вже зареєстрований");
        }

        Set<Role> roles = request.roles().stream()
                .map(Role::valueOf)
                .collect(Collectors.toSet());

        String verificationToken = UUID.randomUUID().toString();

        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .roles(roles)
                .emailVerified(false)
                .verificationToken(verificationToken)
                .onboardingCompleted(false)
                .enabled(false)
                .accountStatus(AccountStatus.PENDING)
                .gdprConsentedAt(LocalDateTime.now())
                .gdprConsentIp(clientIp)
                .build();

        userRepository.save(user);
        if (user.hasRole(Role.MENTOR)) {
            clearMentorCache();
        }

        try {
            emailService.sendVerificationEmail(user.getEmail(), verificationToken);
        } catch (Exception e) {
            log.warn("Verification email failed for: {}", user.getEmail(), e);
        }

        return "Перевірте пошту і підтвердіть email. "
                + "Після підтвердження очікуйте схвалення адміна.";
    }

    // -----------------------------------------------
    // ЛОГІН
    // -----------------------------------------------
    public AuthResponse login(LoginRequest request) {
        if (!recaptchaService.verify(request.captchaToken())) {
            throw AppException.badRequest("Captcha не пройдена. Спробуйте ще раз.");
        }

        User user = userRepository
                .findByEmail(request.email())
                .orElseThrow(() -> AppException.notFound("Користувача не знайдено"));

        if (!user.isEmailVerified()) {
            throw AppException.forbidden("Підтвердіть email перед входом");
        }

        switch (user.getAccountStatus()) {
            case PENDING   -> throw AppException.forbidden("Акаунт очікує схвалення адміністратора");
            case REJECTED  -> throw AppException.forbidden("Акаунт відхилено. Зверніться до адміністратора.");
            case SUSPENDED -> throw AppException.forbidden("Акаунт заблоковано. Зверніться до адміністратора.");
            default -> { /* APPROVED — продовжуємо */ }
        }

        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        String token = jwtUtil.generateToken(user.getEmail());

        Set<String> roleNames = user.getRoles().stream()
                .map(Role::name)
                .collect(Collectors.toSet());

        return new AuthResponse(
                user.getId(),
                token,
                user.getName(),
                user.getEmail(),
                roleNames,
                user.getAccountStatus().name(),
                user.isEmailVerified(),
                user.isOnboardingCompleted()
        );
    }

    // -----------------------------------------------
    // ЗАПРОСИТИ МЕНТОРА
    // -----------------------------------------------
    public void inviteMentor(String email) {
        if (userRepository.existsByEmail(email)) {
            throw AppException.conflict("Користувач з таким email вже існує");
        }

        String inviteToken = UUID.randomUUID().toString();
        User mentor = User.builder()
                .email(email)
                .name("")
                .password("")
                .roles(Set.of(Role.MENTOR))
                .emailVerified(true)
                .enabled(false)
                .accountStatus(AccountStatus.PENDING)
                .inviteToken(inviteToken)
                .build();

        userRepository.save(mentor);
        emailService.sendMentorInvite(email, inviteToken);
    }

    // -----------------------------------------------
    // ЗАВЕРШЕННЯ РЕЄСТРАЦІЇ ПО ЗАПРОШЕННЮ
    // -----------------------------------------------
    public String completeInvite(CompleteInviteRequest request) {
        User user = userRepository
                .findByInviteToken(request.inviteToken())
                .orElseThrow(() -> AppException.badRequest("Невірний або прострочений токен запрошення"));

        if (request.name() == null || request.name().isBlank()) {
            throw AppException.badRequest("Name field is empty!");
        }

        if (request.password() == null || request.password().length() < 6) {
            throw AppException.badRequest("Minimal password length is 6 symbols");
        }

        user.setName(request.name());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setInviteToken(null);
        userRepository.save(user);
        clearMentorCache();

        // Send notification to admin to approve the user
        try {
            emailService.sendNewUserNotification(user.getName(), user.getEmail(), "MENTOR");
        } catch (Exception e) {
            log.warn("Notification email failed for: {}", user.getEmail(), e);
        }

        return "Реєстрацію завершено. Очікуйте схвалення адміністратора.";
    }

    // -----------------------------------------------
    // ЗАВЕРШЕННЯ РЕЄСТРАЦІЇ ЧЛЕНА ОРГАНІЗАЦІЇ
    // -----------------------------------------------
    public String completeOrgMemberInvite(CompleteOrgMemberInviteRequest request) {
        User user = userRepository
                .findByInviteToken(request.inviteToken())
                .orElseThrow(() -> new RuntimeException(
                        "Невірний або прострочений токен запрошення"
                ));
        if (request.name() == null || request.name().isBlank()) {
            throw new RuntimeException("Name field is empty!");
        }

        if (request.password() == null || request.password().length() < 6) {
            throw new RuntimeException("Password length is 6+ symbols");
        }
        user.setName(request.name());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setInviteToken(null);
        user.setEnabled(false);
        user.setAccountStatus(AccountStatus.PENDING);
        userRepository.save(user);

        memberRepository.findAllByUserId(user.getId()).forEach(membership -> {
            Cache cache = cacheManager.getCache(CacheNames.ORG_MEMBERS);
            if (cache != null) {
                cache.evict(membership.getOrganization().getId());
            }
        });

        return "Реєстрацію завершено. Ви можете увійти в систему та прийняти запрошення до команди.";
    }

    public String completeTeamMemberInvite(CompleteOrgMemberInviteRequest request) {
        User user = userRepository
                .findByInviteToken(request.inviteToken())
                .orElseThrow(() -> new RuntimeException(
                        "Невірний або прострочений токен запрошення"
                ));
        if (request.name() == null || request.name().isBlank()) {
            throw new RuntimeException("Name field is empty!");
        }
        if (request.password() == null || request.password().length() < 6) {
            throw new RuntimeException("Password length is 6+ symbols");
        }
        user.setName(request.name());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setInviteToken(null);
        user.setEnabled(false);
        user.setAccountStatus(AccountStatus.PENDING);
        userRepository.save(user);

        try {
            emailService.sendNewUserNotification(
                    user.getName(),
                    user.getEmail(),
                    "STUDENT"
            );
        } catch (Exception e) {
            System.out.println("Admin notification failed");
        }

        return "Реєстрацію завершено. Очікуйте схвалення адміністратора.";
    }


    // -----------------------------------------------
    // СХВАЛЕННЯ АКАУНТУ (тільки SUPER_ADMIN)
    // СХВАЛЕННЯ / ВІДХИЛЕННЯ / БЛОКУВАННЯ АКАУНТУ
    // -----------------------------------------------
    public void approveUser(Long userId, User actor) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("Юзера не знайдено"));

        if (!user.isEmailVerified()) {
            throw AppException.badRequest("Користувач ще не підтвердив email");
        }

        user.setAccountStatus(AccountStatus.APPROVED);
        user.setEnabled(true);
        userRepository.save(user);
        if (user.hasRole(Role.MENTOR)) {
            Cache c = cacheManager.getCache(CacheNames.MENTORS_PUBLIC);
            if (c != null) c.clear();
        }

        emailService.sendAccountApproved(user.getEmail(), user.getName());
        auditService.log(actor, "USER_APPROVED", "USER", userId,
                "Акаунт схвалено: " + user.getEmail());
    }

    public void rejectUser(Long userId, String reason, User actor) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("Юзера не знайдено"));

        user.setAccountStatus(AccountStatus.REJECTED);
        user.setEnabled(false);
        userRepository.save(user);

        if (user.hasRole(Role.MENTOR)) {
            Cache c = cacheManager.getCache(CacheNames.MENTORS_PUBLIC);
            if (c != null) c.clear();
        }

        emailService.sendAccountRejected(user.getEmail(), user.getName(), reason);
        auditService.log(actor, "USER_REJECTED", "USER", userId,
                "Акаунт відхилено: " + user.getEmail() + ". Причина: " + reason);
    }

    public void suspendUser(Long userId, String reason, User actor) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("Юзера не знайдено"));

        user.setAccountStatus(AccountStatus.SUSPENDED);
        user.setEnabled(false);
        userRepository.save(user);

        emailService.sendAccountSuspended(user.getEmail(), user.getName(), reason);
        auditService.log(actor, "USER_SUSPENDED", "USER", userId,
                "Акаунт заблоковано: " + user.getEmail() + ". Причина: " + reason);
    }

    // -----------------------------------------------
    // ДОДАТИ / ЗАБРАТИ РОЛЬ
    // -----------------------------------------------
    public void addRole(Long userId, Role role, User actor) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("Юзера не знайдено"));
        user.getRoles().add(role);
        userRepository.save(user);
        if (role == Role.MENTOR) {
            Cache c = cacheManager.getCache(CacheNames.MENTORS_PUBLIC);
            if (c != null) c.clear();
        }

        auditService.log(actor, "USER_ROLE_ADDED", "USER", userId,
                "Роль " + role.name() + " додано для: " + user.getEmail());

    }

    public void removeRole(Long userId, Role role, User actor) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("Юзера не знайдено"));
        user.getRoles().remove(role);
        userRepository.save(user);
        auditService.log(actor, "USER_ROLE_REMOVED", "USER", userId,
                "Роль " + role.name() + " видалено для: " + user.getEmail());

        userRepository.save(user);
        if (role == Role.MENTOR) {
            Cache c = cacheManager.getCache(CacheNames.MENTORS_PUBLIC);
            if (c != null) c.clear();
        }
        if (role == Role.FIRM) {
            // Deactivate their org memberships (discussed previously)
            memberRepository.findAllByUserId(userId).forEach(m -> {
                Cache orgMembers = cacheManager.getCache(CacheNames.ORG_MEMBERS);
                if (orgMembers != null) orgMembers.evict(m.getOrganization().getId());
            });
            Cache orgs = cacheManager.getCache(CacheNames.ORGANIZATIONS);
            if (orgs != null) orgs.clear();
            Cache orgsPublic = cacheManager.getCache(CacheNames.ORGANIZATIONS_PUBLIC);
            if (orgsPublic != null) orgsPublic.clear();
        }
        auditService.log(actor, "USER_ROLE_REMOVED", "USER", userId,
                "Роль " + role.name() + " видалено для: " + user.getEmail());
    }

    // -----------------------------------------------
    // ПІДТВЕРДЖЕННЯ EMAIL
    // -----------------------------------------------
    public String verifyEmail(String token) {
        User user = userRepository
                .findByVerificationToken(token)
                .orElseThrow(() -> AppException.badRequest("Невірний токен"));

        if (user.isEmailVerified()) {
            return "Email вже підтверджений";
        }

        user.setEmailVerified(true);
        user.setVerificationToken(null);
        userRepository.save(user);

        // Повідомляємо адміна ПІСЛЯ підтвердження email
        try {
            emailService.sendNewUserNotification(
                    user.getName(),
                    user.getEmail(),
                    user.getRoles().stream().map(Role::name).collect(Collectors.joining(", "))
            );
        } catch (Exception e) {
            log.warn("Admin notification failed for: {}", user.getEmail(), e);
        }

        return "Email підтверджено. Очікуйте схвалення адміністратора.";
    }

    // -----------------------------------------------
    // СКИДАННЯ ПАРОЛЯ
    // -----------------------------------------------
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.email())
                .ifPresent(user -> {
                    String resetToken = UUID.randomUUID().toString();
                    user.setResetPasswordToken(resetToken);
                    user.setResetTokenExpiry(LocalDateTime.now().plusHours(1));
                    userRepository.save(user);
                    emailService.sendResetPasswordEmail(user.getEmail(), resetToken);
                });
    }

    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository
                .findByResetPasswordToken(request.token())
                .orElseThrow(() -> AppException.badRequest("Невірний токен"));

        if (user.getResetTokenExpiry() == null
                || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw AppException.badRequest("Токен прострочений");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setResetPasswordToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
    }

    private void clearMentorCache() {
        Cache c = cacheManager.getCache(CacheNames.MENTORS_PUBLIC);
        if (c != null) {
            c.clear();
        }
    }

    public List<UserDTO> getPendingUsers() {
        return userRepository
                .findByAccountStatusAndEmailVerified(AccountStatus.PENDING, true)
                .stream()
                .map(this::toUserDTO)
                .toList();
    }

    public List<UserDTO> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::toUserDTO)
                .toList();
    }

    private UserDTO toUserDTO(User u) {
        return new UserDTO(
                u.getId(),
                u.getName(),
                u.getEmail(),
                u.getRoles().stream()
                        .map(Role::name)
                        .collect(Collectors.toSet()),
                u.getAccountStatus().name(),
                u.isEmailVerified(),
                u.getCreatedAt()
        );
    }
}
