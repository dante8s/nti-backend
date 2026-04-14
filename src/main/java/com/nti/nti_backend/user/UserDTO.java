package com.nti.nti_backend.user;

import java.time.LocalDateTime;
import java.util.Set;

public record UserDTO(
        Long id,
        String name,
        String email,
        Set<String> roles,
        String accountStatus,
        boolean emailVerified,
        LocalDateTime createdAt
) {}