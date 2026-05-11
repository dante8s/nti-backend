package com.nti.nti_backend.studentProfile;

import java.util.Set;

/** Мінімальні дані сесії для фронту (id + ролі) — узгоджено з Jwt / User. */
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
