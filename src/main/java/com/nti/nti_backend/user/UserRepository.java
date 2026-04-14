package com.nti.nti_backend.user;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository
        extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByVerificationToken(
            String verificationToken);

    Optional<User> findByResetPasswordToken(
            String resetPasswordToken);

    // Всі юзери що чекають схвалення
    List<User> findByAccountStatus(AccountStatus status);
}