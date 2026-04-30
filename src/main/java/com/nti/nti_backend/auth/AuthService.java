package com.nti.nti_backend.auth;

import com.nti.nti_backend.email.EmailService;
import com.nti.nti_backend.jwt.JwtUtil;
import com.nti.nti_backend.recaptcha.RecaptchaService;
import com.nti.nti_backend.user.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authManager;
    private final EmailService emailService;
    private final RecaptchaService recaptchaService;

    // Ролі які можна вибрати при реєстрації
    private static final Set<String> ALLOWED_ROLES =
            Set.of("STUDENT", "FIRM", "MENTOR");

    // -----------------------------------------------
    // РЕЄСТРАЦІЯ
    // -----------------------------------------------
    public String register(RegisterRequest request) {
        if (!recaptchaService.verify(request.captchaToken())) {
            throw new RuntimeException("Captcha не пройдена. Спробуйте ще раз.");
        }

        if (!request.gdprConsent()) {
            throw new RuntimeException("Потрібна згода на обробку даних");
        }

        for (String role : request.roles()) {
            if (!ALLOWED_ROLES.contains(role)) {
                throw new RuntimeException("Недозволена роль: " + role);
            }
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new RuntimeException("Email вже зареєстрований");
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
                .build();

        userRepository.save(user);

        try {
            emailService.sendVerificationEmail(user.getEmail(), verificationToken);
        } catch (Exception e) {
            System.out.println("Verification email failed");
        }

//        try {
//            emailService.sendNewUserNotification(
//                    user.getName(),
//                    user.getEmail(),
//                    roles.stream().map(Role::name).collect(Collectors.joining(", "))
//            );
//        } catch (Exception e) {
//            System.out.println("Admin email failed");
//        }

        return "Перевірте пошту і підтвердіть email. "
                + "Після підтвердження очікуйте схвалення адміна.";
    }

    // -----------------------------------------------
    // ЛОГІН — правильний порядок перевірок
    // -----------------------------------------------
    public AuthResponse login(LoginRequest request) {
        if (!recaptchaService.verify(request.captchaToken())) {
            throw new RuntimeException("Captcha не пройдена. Спробуйте ще раз.");
        }

        User user = userRepository
                .findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("Користувача не знайдено"));

        // 1. Спочатку перевіряємо email — найбільш пріоритетно
        if (!user.isEmailVerified()) {
            throw new RuntimeException("Підтвердіть email перед входом");
        }

        // 2. Потім статус акаунту
        switch (user.getAccountStatus()) {
            case PENDING -> throw new RuntimeException(
                    "Акаунт очікує схвалення адміністратора");
            case REJECTED -> throw new RuntimeException(
                    "Акаунт відхилено. Зверніться до адміністратора.");
            case SUSPENDED -> throw new RuntimeException(
                    "Акаунт заблоковано. Зверніться до адміністратора.");
            default -> { /* APPROVED — продовжуємо */ }
        }

        // 3. Spring Security перевіряє пароль (може кинути BadCredentialsException)
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(), request.password()
                )
        );

        String token = jwtUtil.generateToken(user.getEmail());

        Set<String> roleNames = user.getRoles().stream()
                .map(Role::name)
                .collect(Collectors.toSet());

        return new AuthResponse(
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
    // СХВАЛЕННЯ АКАУНТУ (тільки SUPER_ADMIN)
    // -----------------------------------------------
    public void approveUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Юзера не знайдено"));

        user.setAccountStatus(AccountStatus.APPROVED);
        user.setEnabled(true);
        userRepository.save(user);

        emailService.sendAccountApproved(user.getEmail(), user.getName());
    }

    // -----------------------------------------------
    // ВІДХИЛЕННЯ АКАУНТУ (тільки SUPER_ADMIN)
    // -----------------------------------------------
    public void rejectUser(Long userId, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Юзера не знайдено"));

        user.setAccountStatus(AccountStatus.REJECTED);
        user.setEnabled(false);
        userRepository.save(user);

        emailService.sendAccountRejected(user.getEmail(), user.getName(), reason);
    }

    // -----------------------------------------------
    // БЛОКУВАННЯ АКАУНТУ (тільки SUPER_ADMIN)
    // -----------------------------------------------
    public void suspendUser(Long userId, String reason) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Юзера не знайдено"));

        user.setAccountStatus(AccountStatus.SUSPENDED);
        user.setEnabled(false);
        userRepository.save(user);

        emailService.sendAccountSuspended(user.getEmail(), user.getName(), reason);
    }

    // -----------------------------------------------
    // ДОДАТИ / ЗАБРАТИ РОЛЬ (SUPER_ADMIN)
    // -----------------------------------------------
    public void addRole(Long userId, Role role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Юзера не знайдено"));
        user.getRoles().add(role);
        userRepository.save(user);
    }

    public void removeRole(Long userId, Role role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Юзера не знайдено"));
        user.getRoles().remove(role);
        userRepository.save(user);
    }

    // -----------------------------------------------
    // ПІДТВЕРДЖЕННЯ EMAIL
    // -----------------------------------------------
    public String verifyEmail(String token) {
        User user = userRepository
                .findByVerificationToken(token)
                .orElseThrow(() ->
                        new RuntimeException("Невірний токен")
                );

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
                    user.getRoles().stream()
                            .map(Role::name)
                            .collect(Collectors.joining(", "))
            );
        } catch (Exception e) {
            System.out.println("Admin notification failed");
        }

        return "Email підтверджено. "
                + "Очікуйте схвалення адміністратора.";
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
                .orElseThrow(() -> new RuntimeException("Невірний токен"));

        if (user.getResetTokenExpiry() == null
                || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Токен прострочений");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setResetPasswordToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
    }

    public List<UserDTO> getPendingUsers() {
        return userRepository
                .findByAccountStatus(AccountStatus.PENDING)
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