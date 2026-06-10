package com.nti.nti_backend.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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

    // All users awaiting approval
    List<User> findByAccountStatus(AccountStatus status);

    // Тільки ті що підтвердили email і чекають схвалення
    List<User> findByAccountStatusAndEmailVerified(AccountStatus status, boolean emailVerified);

    // Find All Users with role
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r = :role")
    List<User> findAllByRole(Role role);

    // Invite Token
    Optional<User> findByInviteToken(String inviteToken);
    boolean existsByInviteToken(String inviteToken);
}