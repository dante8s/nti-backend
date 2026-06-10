package com.nti.nti_backend.studentProfile;

import java.util.Set;

/** Minimal session data for the frontend (id + roles) — aligned with Jwt / User. */
public record ProfileSessionBrief(
        Long userId,
        String name,
        String email,
        Set<String> roles,
        String accountStatus,
        boolean emailVerified,
        boolean onboardingCompleted
) {
}
