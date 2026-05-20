package com.nti.nti_backend.auth;

import java.util.Set;

public record AuthResponse(
        Long id,                     // id користувача для фронту (команди, запрошення тощо)
        String token,
        String name,
        String email,
        Set<String> roles,           // список ролей
        String accountStatus,        // PENDING / APPROVED
        boolean emailVerified,
        boolean onboardingCompleted
) {}