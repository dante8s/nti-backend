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



    // Roles available for selection during registration
    private static final Set<String> ALLOWED_ROLES =
            Set.of("STUDENT", "FIRM", "MENTOR");

    // -----------------------------------------------
    // REGISTRATION
    // -----------------------------------------------
    public String register(RegisterRequest request, String clientIp) {
        if (!recaptchaService.verify(request.captchaToken())) {
            throw AppException.badRequest("Captcha failed. Please try again.");
        }

        if (!request.gdprConsent()) {
            throw AppException.badRequest("Data processing consent is required");
        }

        if (!request.email().toLowerCase().endsWith("@student.ukf.sk")) {
            throw AppException.badRequest("Registration is only allowed for @student.ukf.sk addresses");
        }

        for (String role : request.roles()) {
            if (!ALLOWED_ROLES.contains(role)) {
                throw AppException.badRequest("Disallowed role: " + role);
            }
        }

        if (userRepository.existsByEmail(request.email())) {
            throw AppException.conflict("Email is already registered");
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

        return "Check your email and confirm your address. "
                + "After confirmation, await administrator approval.";
    }

    // -----------------------------------------------
    // LOGIN
    // -----------------------------------------------
    public AuthResponse login(LoginRequest request) {
        if (!recaptchaService.verify(request.captchaToken())) {
            throw AppException.badRequest("Captcha failed. Please try again.");
        }

        User user = userRepository
                .findByEmail(request.email())
                .orElseThrow(() -> AppException.notFound("User not found"));

        if (!user.isEmailVerified()) {
            throw AppException.forbidden("Please verify your email before logging in");
        }

        switch (user.getAccountStatus()) {
            case PENDING   -> throw AppException.forbidden("Account is awaiting administrator approval");
            case REJECTED  -> throw AppException.forbidden("Account has been rejected. Please contact the administrator.");
            case SUSPENDED -> throw AppException.forbidden("Account is suspended. Please contact the administrator.");
            default -> { /* APPROVED — continue */ }
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
    // INVITE MENTOR
    // -----------------------------------------------
    public void inviteMentor(String email) {
        if (userRepository.existsByEmail(email)) {
            throw AppException.conflict("A user with this email already exists");
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
    // COMPLETE REGISTRATION VIA INVITE
    // -----------------------------------------------
    public String completeInvite(CompleteInviteRequest request) {
        User user = userRepository
                .findByInviteToken(request.inviteToken())
                .orElseThrow(() -> AppException.badRequest("Invalid or expired invite token"));

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

        return "Registration complete. Awaiting administrator approval.";
    }

    // -----------------------------------------------
    // COMPLETE ORGANIZATION MEMBER REGISTRATION
    // -----------------------------------------------
    public String completeOrgMemberInvite(CompleteOrgMemberInviteRequest request) {
        User user = userRepository
                .findByInviteToken(request.inviteToken())
                .orElseThrow(() -> new RuntimeException(
                        "Invalid or expired invite token"
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

        return "Registration complete. You can now log in and accept the team invitation.";
    }

    public String completeTeamMemberInvite(CompleteOrgMemberInviteRequest request) {
        User user = userRepository
                .findByInviteToken(request.inviteToken())
                .orElseThrow(() -> new RuntimeException(
                        "Invalid or expired invite token"
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

        return "Registration complete. Awaiting administrator approval.";
    }


    // -----------------------------------------------
    // APPROVE ACCOUNT (SUPER_ADMIN only)
    // APPROVE / REJECT / SUSPEND ACCOUNT
    // -----------------------------------------------
    public void approveUser(Long userId, User actor) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("User not found"));

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
                "Account approved: " + user.getEmail());
    }

    public void rejectUser(Long userId, String reason, User actor) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("User not found"));

        user.setAccountStatus(AccountStatus.REJECTED);
        user.setEnabled(false);
        userRepository.save(user);

        if (user.hasRole(Role.MENTOR)) {
            Cache c = cacheManager.getCache(CacheNames.MENTORS_PUBLIC);
            if (c != null) c.clear();
        }

        emailService.sendAccountRejected(user.getEmail(), user.getName(), reason);
        auditService.log(actor, "USER_REJECTED", "USER", userId,
                "Account rejected: " + user.getEmail() + ". Reason: " + reason);
    }

    public void suspendUser(Long userId, String reason, User actor) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("User not found"));

        user.setAccountStatus(AccountStatus.SUSPENDED);
        user.setEnabled(false);
        userRepository.save(user);

        emailService.sendAccountSuspended(user.getEmail(), user.getName(), reason);
        auditService.log(actor, "USER_SUSPENDED", "USER", userId,
                "Account suspended: " + user.getEmail() + ". Reason: " + reason);
    }

    // -----------------------------------------------
    // ADD / REMOVE ROLE
    // -----------------------------------------------
    public void addRole(Long userId, Role role, User actor) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("User not found"));
        user.getRoles().add(role);
        userRepository.save(user);
        if (role == Role.MENTOR) {
            Cache c = cacheManager.getCache(CacheNames.MENTORS_PUBLIC);
            if (c != null) c.clear();
        }

        auditService.log(actor, "USER_ROLE_ADDED", "USER", userId,
                "Role " + role.name() + " added for: " + user.getEmail());

    }

    public void removeRole(Long userId, Role role, User actor) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("User not found"));
        user.getRoles().remove(role);
        userRepository.save(user);
        auditService.log(actor, "USER_ROLE_REMOVED", "USER", userId,
                "Role " + role.name() + " removed for: " + user.getEmail());

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
                "Role " + role.name() + " removed for: " + user.getEmail());
    }

    // -----------------------------------------------
    // EMAIL VERIFICATION
    // -----------------------------------------------
    public String verifyEmail(String token) {
        User user = userRepository
                .findByVerificationToken(token)
                .orElseThrow(() -> AppException.badRequest("Invalid token"));

        if (user.isEmailVerified()) {
            return "Email is already verified";
        }

        user.setEmailVerified(true);
        user.setVerificationToken(null);
        userRepository.save(user);

        // Notify admin AFTER email verification
        try {
            emailService.sendNewUserNotification(
                    user.getName(),
                    user.getEmail(),
                    user.getRoles().stream().map(Role::name).collect(Collectors.joining(", "))
            );
        } catch (Exception e) {
            log.warn("Admin notification failed for: {}", user.getEmail(), e);
        }

        return "Email verified. Awaiting administrator approval.";
    }

    // -----------------------------------------------
    // PASSWORD RESET
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
                .orElseThrow(() -> AppException.badRequest("Invalid token"));

        if (user.getResetTokenExpiry() == null
                || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw AppException.badRequest("Token has expired");
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
