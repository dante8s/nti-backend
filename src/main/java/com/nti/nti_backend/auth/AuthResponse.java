package com.nti.nti_backend.auth;

import java.util.Set;

public record AuthResponse(
        Long id,                     // user id for frontend (teams, invitations, etc.)
        String token,
        String name,
        String email,
        Set<String> roles,           // list of roles
        String accountStatus,        // PENDING / APPROVED
        boolean emailVerified,
        boolean onboardingCompleted
) {}