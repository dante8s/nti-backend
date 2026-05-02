package com.nti.nti_backend.user;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    // Кілька ролей — зберігаються в окремій таблиці user_roles
    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @Column(name = "role")
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    // Email підтвердження
    @Builder.Default
    private boolean emailVerified = false;
    private String verificationToken;

    // Invite token for mentor
    private String inviteToken;

    // Скидання пароля
    private String resetPasswordToken;
    private LocalDateTime resetTokenExpiry;

    // Онбординг
    @Builder.Default
    private boolean onboardingCompleted = false;

    // ВАЖЛИВО: enabled = false до схвалення адміном
    @Builder.Default
    private boolean enabled = false;

    // Статус схвалення адміном
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AccountStatus accountStatus = AccountStatus.PENDING;

    @CreationTimestamp
    private LocalDateTime createdAt;

    // --- UserDetails ---

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(
                        "ROLE_" + role.name()
                ))
                .collect(Collectors.toList());
    }

    // Зручний метод — перевірити чи має роль
    public boolean hasRole(Role role) {
        return roles.contains(role);
    }

    @Override
    public String getUsername() { return email; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return enabled; }
}