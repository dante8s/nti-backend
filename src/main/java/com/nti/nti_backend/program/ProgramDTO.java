package com.nti.nti_backend.program;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProgramDTO(
        Long id,
        String name,
        String description,
        String type,
        String status,
        String adminComment,
        UUID organizationId,
        String organizationName,
        LocalDateTime updatedAt
) {}