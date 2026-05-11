package com.nti.nti_backend.application;

import java.time.LocalDateTime;
import java.util.UUID;

public record ApplicationDTO(
        Long id,
        Long callId,
        String callTitle,
        String programName,
        String programType,
        String status,
        String adminComment,
        UUID organizationId,
        String organizationName,
        Long productOwnerId,
        String productOwnerName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}