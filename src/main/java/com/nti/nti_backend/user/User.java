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

    // Multiple roles — stored in a separate user_roles table
    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id")
    )
    @Column(name = "role")
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    // Email verification
    @Builder.Default
    private boolean emailVerified = false;
    private String verificationToken;

    // Invite token for mentor
    private String inviteToken;

    // Password reset
    private String resetPasswordToken;
    private LocalDateTime resetTokenExpiry;

    // Onboarding
    @Builder.Default
    private boolean onboardingCompleted = false;

    // IMPORTANT: enabled = false until approved by admin
    @Builder.Default
    private boolean enabled = false;

    // Admin approval status
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AccountStatus accountStatus = AccountStatus.PENDING;

    @CreationTimestamp
    private LocalDateTime createdAt;

    private LocalDateTime gdprConsentedAt;
    private String        gdprConsentIp;

    // --- UserDetails ---

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(
                        "ROLE_" + role.name()
                ))
                .collect(Collectors.toList());
    }

    // Convenience method — check if user has a role
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