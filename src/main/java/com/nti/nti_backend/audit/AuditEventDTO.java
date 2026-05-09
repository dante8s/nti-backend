package com.nti.nti_backend.audit;

import java.time.LocalDateTime;

public record AuditEventDTO(
        Long id,
        String actorName,
        String action,
        String description,
        LocalDateTime createdAt
) {}